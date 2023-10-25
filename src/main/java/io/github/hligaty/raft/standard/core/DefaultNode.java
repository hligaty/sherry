package io.github.hligaty.raft.standard.core;

import io.github.hligaty.raft.standard.Node;
import io.github.hligaty.raft.standard.config.Configuration;
import io.github.hligaty.raft.standard.rpc.RpcRequest;
import io.github.hligaty.raft.standard.rpc.RpcService;
import io.github.hligaty.raft.standard.rpc.SofaBoltService;
import io.github.hligaty.raft.standard.rpc.packet.AppendEntryRequest;
import io.github.hligaty.raft.standard.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.standard.rpc.packet.RequestVoteResponse;
import io.github.hligaty.raft.standard.storage.LogId;
import io.github.hligaty.raft.standard.storage.LogRepository;
import io.github.hligaty.raft.standard.storage.RocksDBRepository;
import io.github.hligaty.raft.standard.util.Endpoint;
import io.github.hligaty.raft.standard.util.RepeatedTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultNode implements Node {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultNode.class);

    private Configuration configuration;

    private Endpoint endpoint;

    private RpcService rpcService;

    private volatile State state;

    private final Lock writeLock;
    private final Lock readLock;

    private long currTerm;
    private Endpoint votedEndpoint;

    private volatile long lastLeaderTimestamp;

    private final ExecutorService executorService;

    private final LogRepository logRepository;

    public DefaultNode() {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.writeLock = readWriteLock.writeLock();
        this.readLock = readWriteLock.readLock();

        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        this.logRepository = new RocksDBRepository();
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        this.endpoint = configuration.getEndpoint();
    }

    @Override
    public void start() {
        state = State.FOLLOWER;
        rpcService = new SofaBoltService(endpoint.port(), this);
        RepeatedTimer electionTimeoutTimer = new RepeatedTimer("electionTimeoutTimer") {

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
        electionTimeoutTimer.start();
    }

    @Override
    public RequestVoteResponse handleVoteRequest(RequestVoteRequest request) {
        writeLock.lock();
        try {
            if (
                    currTerm <= request.term() // 比自己的任期小, 拒绝
                    || System.currentTimeMillis() - lastLeaderTimestamp >= configuration.getElectionTimeoutMs() // 领导者最后一条消息就在刚刚不久前, 拒绝
                    || votedEndpoint != null // 当前任期已经投过票了
                    || new LogId(request.lastLogIndex(), request.term()).compareTo(logRepository.getLastLogId()) >= 0 // 当前节点日志超过候选者
            ) {
                return new RequestVoteResponse(currTerm, false);
            }
            // 赞成投票
            state = State.FOLLOWER;
            currTerm = request.term();
            votedEndpoint = request.endpoint();
            return new RequestVoteResponse(currTerm, true);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean appendEntry(AppendEntryRequest appendEntryRequest) {
        return false;
    }

    private void electionSelf() {
        if (
                state != State.FOLLOWER // 是跟随者才尝试变成候选者
                // election_timeout_ms 内没有收到领导者的消息, 才尝试变成候选者(因为心跳等消息的时间间隔远小于 election_timeout_ms)
                || System.currentTimeMillis() - lastLeaderTimestamp < configuration.getElectionTimeoutMs()
        ) {
            return;
        }
        // 开始选举自己
        writeLock.lock();
        try {
            long term = currTerm + 1;
            LogId lastLogId = logRepository.getLastLogId();
            LongAdder voteCount = new LongAdder();
            CompletableFuture
                    .allOf(
                            configuration.getOtherEndpoints().stream()
                                    .map(otherEndpoint -> CompletableFuture.runAsync(() -> {
                                        RequestVoteRequest request = new RequestVoteRequest(otherEndpoint, term, lastLogId.index(), lastLogId.term());
                                        RpcRequest rpcRequest = new RpcRequest(otherEndpoint, request, configuration.getElectionTimeoutMs());
                                        if (rpcService.sendRequest(rpcRequest) instanceof RequestVoteResponse response) {
                                            if (response.term() > currTerm) { // 其他人的任期更高, 更新任期
                                                currTerm = response.term();
                                            } else if (response.granted()) { // 其他人投了赞成票
                                                voteCount.increment();
                                            }
                                        }
                                    }, executorService).exceptionally(throwable -> {
                                        LOG.info("An error occurred while requesting another node to vote, node:[{}]", otherEndpoint, throwable);
                                        return null;
                                    }))
                                    .toArray(CompletableFuture[]::new))
                    .join();
            if (voteCount.sum() > (configuration.getOtherEndpoints().size() + 1) / 2) { // 超过一半节点认为应该成为领导者
                state = State.LEADER;
                // 成为领导者后开始复制日志
            }
        } finally {
            writeLock.unlock();
        }
    }
}
