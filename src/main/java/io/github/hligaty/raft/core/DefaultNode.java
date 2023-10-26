package io.github.hligaty.raft.core;

import io.github.hligaty.raft.Node;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.rpc.RpcRequest;
import io.github.hligaty.raft.rpc.RpcService;
import io.github.hligaty.raft.rpc.SofaBoltService;
import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.AppendEntriesResponse;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;
import io.github.hligaty.raft.storage.LogId;
import io.github.hligaty.raft.storage.LogRepository;
import io.github.hligaty.raft.storage.RocksDBRepository;
import io.github.hligaty.raft.util.Endpoint;
import io.github.hligaty.raft.util.RepeatedTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DefaultNode implements Node {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultNode.class);

    private Configuration configuration;

    private RpcService rpcService;

    private State state;

    private final Lock lock;

    private long currTerm;

    private Endpoint votedEndpoint;

    private volatile long lastLeaderTimestamp;

    private final ExecutorService virtualThreadPerTaskExecutor;

    private final LogRepository logRepository;

    private RepeatedTimer electionTimer;

    private RepeatedTimer heartbeatTimer;

    public DefaultNode() {
        this.lock = new ReentrantLock();
        this.virtualThreadPerTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.logRepository = new RocksDBRepository();
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void startup() {
        state = State.FOLLOWER;
        rpcService = new SofaBoltService(configuration, this);
        electionTimer = new RepeatedTimer("electionTimer") {

            @Override
            protected int adjustTimeout() {
                return configuration.getElectionTimeoutMs()
                       + ThreadLocalRandom.current().nextInt(0, configuration.getMaxElectionDelayMs());
            }

            @Override
            protected void onTrigger() {
                electionSelf();
            }
        };
        electionTimer.start();
        heartbeatTimer = new RepeatedTimer("heartbeatTimer") {

            @Override
            protected int adjustTimeout() {
                return configuration.getHeartbeatTimeoutMs();
            }

            @Override
            protected void onTrigger() {
                heartbeat();
            }
        };
    }

    @Override
    public RequestVoteResponse handleVoteRequest(RequestVoteRequest request) {
        lock.lock();
        try {
            if (
                    currTerm <= request.term() // 比自己的任期小, 拒绝
                    // 领导者最后一条消息就在刚刚不久前, 拒绝投票. 防止非对称网络分区中的跟随者任期不断增加导致领导者变更的问题
                    || System.currentTimeMillis() - lastLeaderTimestamp >= configuration.getElectionTimeoutMs()
                    || votedEndpoint != null // 当前任期已经投过票了
                    || new LogId(request.lastLogIndex(), request.term()).compareTo(logRepository.getLastLogId()) >= 0 // 当前节点日志超过候选者
            ) {
                return new RequestVoteResponse(currTerm, false);
            }
            // 赞成投票
            state = State.FOLLOWER;
            currTerm = request.term();
            votedEndpoint = request.endpoint();
            electionTimer.start(); // 开启跟随者的选举超时检测
            return new RequestVoteResponse(currTerm, true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean handleAppendEntries(AppendEntriesRequest appendEntriesRequest) {
        // TODO: 2023/10/26 实现日志合并 
        return false;
    }

    private void electionSelf() {
        lock.lock();
        try {
            if (
                    state != State.FOLLOWER // 必须是跟随者才尝试变成候选者.
                    // election_timeout_ms 内没有收到领导者的消息, 才尝试变成候选者(因为心跳等消息的时间间隔远小于 election_timeout_ms)
                    || System.currentTimeMillis() - lastLeaderTimestamp < configuration.getElectionTimeoutMs()
            ) {
                return;
            }
            // 开始选举自己
            state = State.CANDIDATE;
            votedEndpoint = configuration.getEndpoint();
            LongAdder voteCount = new LongAdder(); // 投票计数
            LogId lastLogId = logRepository.getLastLogId();
            RequestVoteRequest request = new RequestVoteRequest(configuration.getEndpoint(), currTerm + 1, lastLogId.index(), lastLogId.term());
            sendRequestAllOf(
                    endpoint -> {
                        RpcRequest rpcRequest = new RpcRequest(endpoint, request, configuration.getElectionTimeoutMs());
                        // 发起投票请求
                        if (rpcService.sendRequest(rpcRequest) instanceof RequestVoteResponse response) {
                            if (response.term() > currTerm) { // 其他人的任期更高, 更新任期
                                currTerm = response.term();
                                votedEndpoint = null;
                            } else if (response.granted()) { // 其他人投了赞成票
                                voteCount.increment();
                            }
                        }
                    },
                    (endpoint, throwable) -> LOG.info("An error occurred while requesting another node to vote, node:[{}]", endpoint, throwable)
            );
            if (voteCount.sum() + 1 >= (configuration.getOtherEndpoints().size() + 1) / 2) { // 超过一半节点认为应该成为领导者
                state = State.LEADER;
                currTerm++; // 当前实现可以解决非对称网络分区任期一直增加的问题吗? 为什么 sofaJRaft 预投票需要两次? 也许是并发的考量?
                votedEndpoint = configuration.getEndpoint();
                electionTimer.stop();
                heartbeatTimer.start();
            } else {
                state = State.FOLLOWER;
                votedEndpoint = null;
            }
        } finally {
            lock.unlock();
        }
    }

    private void heartbeat() {
        // 心跳超时远小于投票超时, 需要把这个锁细化一下, 否则 lastLeaderTimestamp 更新太慢很容易就发生投票了
        lock.lock();
        try {
            if (state != State.LEADER) {
                heartbeatTimer.stop();
                electionTimer.start();
                return;
            }
            LongAdder voteCount = new LongAdder(); // 投票计数
            AppendEntriesRequest request = new AppendEntriesRequest(currTerm, List.of());
            sendRequestAllOf(
                    endpoint -> {
                        RpcRequest rpcRequest = new RpcRequest(endpoint, request, configuration.getHeartbeatTimeoutMs());
                        if (rpcService.sendRequest(rpcRequest) instanceof AppendEntriesResponse response) {
                            if (response.term() > currTerm) {
                                voteCount.increment();
                            }
                        }
                    },
                    (endpoint, throwable) -> LOG.info("An error occurred while requesting another node to heartbeat, node:[{}]", endpoint, throwable)
            );
            if (voteCount.sum() + 1 >= (configuration.getOtherEndpoints().size() + 1) / 2) {
                state = State.FOLLOWER;
                votedEndpoint = null;
            }
        } finally {
            lock.unlock();
        }
    }

    private void sendRequestAllOf(Consumer<Endpoint> requestHandler, BiConsumer<Endpoint, Throwable> exceptionHandler) {
        CompletableFuture
                .allOf(
                        configuration.getOtherEndpoints().stream()
                                .map(endpoint -> CompletableFuture
                                        .runAsync(() -> requestHandler.accept(endpoint), virtualThreadPerTaskExecutor)
                                        .exceptionally(throwable -> {
                                            exceptionHandler.accept(endpoint, throwable);
                                            return null;
                                        }))
                                .toArray(CompletableFuture[]::new))
                .join();
    }
}
