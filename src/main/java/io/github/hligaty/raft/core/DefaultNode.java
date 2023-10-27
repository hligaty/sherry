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
import io.github.hligaty.raft.storage.StoreException;
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

    private long lastLeaderTimestamp;

    private final ExecutorService virtualThreadPerTaskExecutor;

    private LogRepository logRepository;

    private RepeatedTimer electionTimer;

    private RepeatedTimer heartbeatTimer;

    public DefaultNode() {
        this.lock = new ReentrantLock();
        this.virtualThreadPerTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void startup() {
        state = State.FOLLOWER;
        lastLeaderTimestamp = System.currentTimeMillis();
        rpcService = new SofaBoltService(configuration, this);
        logRepository = new RocksDBRepository(configuration);
        electionTimer = new RepeatedTimer("electionTimer") {

            @Override
            protected int adjustTimeout() {
                return configuration.getElectionTimeoutMs()
                       + ThreadLocalRandom.current().nextInt(0, configuration.getMaxElectionDelayMs());
            }

            @Override
            protected void onTrigger() {
                electionSelf(true);
            }
        };
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
        electionTimer.start();
    }

    @Override
    public RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest request) {
        lock.lock();
        try {
            if (
                    currTerm <= request.term() // 比自己的任期小, 拒绝
                    // 领导者最后一条消息就在刚刚不久前, 拒绝投票. 防止非对称网络分区中的跟随者任期不断增加导致领导者变更的问题
                    || System.currentTimeMillis() - lastLeaderTimestamp >= configuration.getElectionTimeoutMs()
                    || votedEndpoint != null // 当前任期已经投过票了
                    || new LogId(request.lastLogIndex(), request.term()).compareTo(logRepository.getLastLogId()) < 0 // 当前节点日志超过候选者
            ) {
                return new RequestVoteResponse(currTerm, false);
            }
            if (!request.preVote()) {
                // 赞成投票
                state = State.FOLLOWER;
                currTerm = request.term();
                votedEndpoint = request.endpoint();
                electionTimer.start(); // 开启跟随者的选举超时检测
            }
            return new RequestVoteResponse(currTerm, true);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings ("ConstantConditions")
    @Override
    public AppendEntriesResponse handleAppendEntriesRequest(AppendEntriesRequest request) {
        lock.lock();
        try {
            boolean success = false;
            do {
                if (request.term() < currTerm) {
                    LOG.info("忽略过时的 AppendEntriesRequest, term=[{}], currTerm=[{}]", request.term(), currTerm);
                    break;
                }
                lastLeaderTimestamp = System.currentTimeMillis();
                if (state != State.FOLLOWER) { // 任期大于等于当前节点, 相等时集群只有一个领导者, 那么该节点为候选者; 大于则说明网络分区, 同样是候选者
                    LOG.info("候选者任期[{}]收到领导者高任期[{}]的追加日志请求, 拒绝并下降为跟随者", currTerm, request.term());
                    state = State.FOLLOWER;
                    votedEndpoint = null;
                    electionTimer.start();
                    break;
                }
                if (request.logEntries().isEmpty()) {
                    LOG.info("收到心跳请求, 更新领导者最后消息时间戳");
                    success = true;
                    break;
                }
                try {
                    logRepository.appendEntries(request.logEntries());
                    success = true;
                } catch (StoreException e) {
                    LOG.error("追加日志失败, 日志内容:[{}]", request.logEntries());
                }
            } while (false);
            return new AppendEntriesResponse(currTerm, success);
        } finally {
            lock.unlock();
        }
    }

    private void electionSelf(boolean preVote) {
        lock.lock();
        try {
            if (
                    state != State.FOLLOWER // 必须是跟随者才尝试变成候选者.
                    // election_timeout_ms 内没有收到领导者的消息, 才尝试变成候选者(因为心跳等消息的时间间隔远小于 election_timeout_ms)
                    || System.currentTimeMillis() - lastLeaderTimestamp < configuration.getElectionTimeoutMs()
            ) {
                return;
            }
            if (!preVote) { // 正式投票时更改状态和当前任期的投票
                LOG.info("投票超时, 发起预投票, 当前任期:[{}]", currTerm);
                state = State.CANDIDATE;
                votedEndpoint = configuration.getEndpoint();
            } else {
                LOG.info("投票超时, 发起正式投票, 当前任期:[{}]", currTerm);
            }
            LongAdder voteCount = new LongAdder(); // 赞成票计数
            voteCount.increment();
            LogId lastLogId = logRepository.getLastLogId();
            RequestVoteRequest request = new RequestVoteRequest(
                    configuration.getEndpoint(),
                    preVote ? currTerm + 1 : ++currTerm, // 预投票不改变任期, 正式投票才改变任期
                    lastLogId.index(),
                    lastLogId.term(),
                    preVote
            );
            // 在发生网络分区时 RPC 超时会占用锁很长时间, 可以细化锁, 但需要加 try finally 和 ABA 判断, 这是一个不太影响 Raft 算法, 但需要考虑的问题
            sendRequestAllOf(
                    endpoint -> {
                        RpcRequest rpcRequest = new RpcRequest(endpoint, request, configuration.getElectionTimeoutMs());
                        // 发起投票请求
                        if (rpcService.sendRequest(rpcRequest) instanceof RequestVoteResponse response) {
                            if (response.term() > currTerm) { // 其他人的任期更高, 回退为跟随者
                                LOG.info("其他节点[{}]任期[{}]更高, 当前节点准备回退为跟随者. 当前节点任期:[{}]", endpoint, response.term(), currTerm);
                                voteCount.add(-mostNodes());
                            } else if (response.granted()) {
                                LOG.info("其他节点[{}]赞同投票. 当前节点任期:[{}]", endpoint, currTerm);
                                voteCount.increment();
                            } else {
                                LOG.info("其他节点[{}]任期小, 但日志更多, 不投票. 当前节点任期:[{}], ", endpoint, currTerm);
                            }
                        }
                    },
                    (endpoint, throwable) -> LOG.info("请求另一个节点投票时出错. 节点:[{}]", endpoint, throwable)
            );
            if (request.preVote()) { // 预投票结果判断
                if (voteCount.sum() >= mostNodes()) {
                    LOG.info("预投票票数[{}]超过一半节点, 开始正式投票", voteCount.sum());
                    lock.unlock();
                    electionSelf(false);
                } else {
                    LOG.info("预投票票数[{}]少于一半节点, 投票失败", voteCount.sum());
                }
            } else if (voteCount.sum() >= mostNodes()) {
                LOG.info("正式投票票数[{}]超过一半节点, 上升为领导者", voteCount.sum());
                state = State.LEADER;
                votedEndpoint = null;
                // TODO: 2023/10/27 获取每个节点的 lastLogIndex 后追加日志 
                electionTimer.stop();
                heartbeatTimer.start();
            } else {
                LOG.info("正式投票票数[{}]少于一半节点, 下降为跟随者", voteCount.sum());
                state = State.FOLLOWER;
                votedEndpoint = null;
            }
        } finally {
            lock.unlock();
        }
    }

    private long mostNodes() {
        return (configuration.getOtherEndpoints().size() + 1) / 2;
    }

    private void heartbeat() {
        lock.lock();
        try {
            if (state != State.LEADER) {
                heartbeatTimer.stop();
                electionTimer.start();
                return;
            }
            LongAdder voteCount = new LongAdder(); // 反对票计数
            AppendEntriesRequest request = new AppendEntriesRequest(currTerm, List.of());
            sendRequestAllOf(
                    endpoint -> {
                        RpcRequest rpcRequest = new RpcRequest(endpoint, request, configuration.getHeartbeatTimeoutMs());
                        if (rpcService.sendRequest(rpcRequest) instanceof AppendEntriesResponse response) {
                            if (response.term() > currTerm) { // 其他节点的任期大, 直接下降为跟随者
                                voteCount.add(-mostNodes());
                            }
                        }
                    },
                    (endpoint, throwable) -> {
                        LOG.info("请求另一个节点[{}]检测信号时出错, 网络分区", endpoint, throwable);
                        voteCount.increment(); // 节点没有响应, 网络分区, 即不赞成该继续担任领导者
                    }
            );
            if (voteCount.sum() >= mostNodes()) { // 大多数节点不赞成或网络分区, 下降为跟随者
                state = State.FOLLOWER;
                votedEndpoint = null;
                heartbeatTimer.stop();
                electionTimer.start();
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
