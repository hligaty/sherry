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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * 配置信息
     */
    private Configuration configuration;

    /**
     * RPC 服务
     */
    private RpcService rpcService;

    private final ExecutorService virtualThreadPerTaskExecutor;

    /**
     * 日志存储
     */
    private LogRepository logRepository;

    // ----------------------------------------------------------------------------------------------------

    /**
     * 全局锁, 控制 Raft 需要的状态等数据的变动. 两个分割线内的数据需要通过锁变更
     */
    private final Lock lock;

    /**
     * 状态
     */
    private State state;

    /**
     * 当前任期
     */
    private long currTerm;

    /**
     * 当前任期的投票
     */
    private Endpoint votedEndpoint;

    /**
     * 当前节点为领导者时使用, 记录每个跟随者匹配到的最后一个日志
     */
    private final Map<Endpoint, Long> matchIndexes = new ConcurrentHashMap<>();

    /**
     * 当前节点为领导者时使用, 记录要发送给每个跟随者的下一个日志
     */
    private final Map<Endpoint, Long> nextIndexes = new ConcurrentHashMap<>();

    //----------------------------------------------------------------------------------------------------

    /**
     * 领导者最后一个消息的时间戳
     */
    private long lastLeaderTimestamp;

    /**
     * 选举计时器, 检测到超时后开始预投票. 当前节点是跟随者时开启
     */
    private RepeatedTimer electionTimer;

    /**
     * 心跳计时器, 向所有节点发送心跳, 表明当前节点还是领导者. 当前节点是领导者时开启
     */
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
                handleElectionTimeout(true);
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
    public <T extends Serializable> void apply(T data) {
        lock.lock();
        try {
            // TODO: 2023/10/30 应用 
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest request) {
        lock.lock();
        try {
            boolean granted = false;
            do {
                if (request.term() < currTerm) { // 比自己的任期小, 拒绝
                    LOG.info("收到投票请求, 但任期[{}]比当前节点任期[{}]小, 拒绝", request.term(), currTerm);
                    break;
                }
                if (request.preVote() && isCurrentLeaderValid()) { // 预投票阶段领导者有效就拒绝. 正式投票时不判断, 因为预投票通过表示大部分节点都同意了
                    LOG.info("收到预投票请求, 但领导者有效, 拒绝");
                    break;
                }
                if (!request.preVote() && request.term() > currTerm && state != State.FOLLOWER) { // 正式投票阶段发现任期大就变更状态
                    LOG.info("正式投票请求的任期[{}]比当前节点任期[{}]大, 更改当前节点状态为跟随者, 当前节点状态为[{}]",
                            request.term(), currTerm, state);
                    assert state == State.LEADER; // 一定是领导者, 因为用了全局锁, 候选者状态只在 electionSelf() 方法中
                    state = State.FOLLOWER;
                    currTerm = request.term();
                    votedEndpoint = null;
                    electionTimer.start();
                    heartbeatTimer.stop();
                }
                if (!request.preVote() && votedEndpoint != null) { // 正式投票时判断是否已经投过了. 预投票只是为了判断是否可以发起正式投票, 不用记录投了谁
                    LOG.info("任期[{}]的正式投票已经投给了[{}], 拒绝其他投票", currTerm, votedEndpoint);
                    break;
                }
                /*
                 * 候选者日志没超过当前节点日志, 拒绝
                 * 这个是 Raft 的核心思想, 只有大多数节点复制日志成功才算成功, 因此, 基于该条件的投票机制也就能保证收到大多数票的那个节点有最新的日志
                 */
                if (new LogId(request.lastLogIndex(), request.term()).compareTo(logRepository.getLastLogId()) < 0) {
                    break;
                }
                granted = true;
                if (!request.preVote()) { // 正式投票阶段投票后更改状态为跟随者
                    state = State.FOLLOWER;
                    currTerm = request.term();
                    votedEndpoint = request.endpoint();
                    electionTimer.start();
                    heartbeatTimer.stop();
                }
            } while (false);
            return new RequestVoteResponse(currTerm, granted);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public AppendEntriesResponse handleAppendEntriesRequest(AppendEntriesRequest request) {
        lock.lock();
        try {
            boolean success = false;
            long lastLogIndex = 0;
            do {
                if (request.term() < currTerm) {
                    LOG.info("忽略过期的追加日志请求, 过期任期=[{}], 当前任期=[{}]", request.term(), currTerm);
                    break;
                }
                lastLeaderTimestamp = System.currentTimeMillis();
                if (state != State.FOLLOWER) {
                    LOG.info("领导者任期[{}]收到新领导者高任期[{}]的追加日志请求, 下降为跟随者", currTerm, request.term());
                    assert state == State.LEADER; // 一定是领导者(因为用了全局锁, 候选者状态只在 electionSelf() 方法中)
                    assert request.term() > currTerm; // 并且任期一定比当前节点大
                    state = State.FOLLOWER;
                    votedEndpoint = null;
                    electionTimer.start();
                    heartbeatTimer.stop();
                }
                /*
                解决这个问题: 网络分区后, 旧领导者的日志复制了少部分节点后才意识到不是领导者了(但日志已经过去了), 然后变成跟随者, 这个日志需要删除
                但这部分日志只能由新领导者与分区的节点协商, 找到共同的日志 A 后删除日志 A 后面的日志, 并复制新领导者日志 A 后面的日志来解决
                 */
                long localPrevLogTerm = logRepository.getTerm(request.prevLogIndex());
                if (request.prevLogTerm() != localPrevLogTerm) {
                    LOG.info("本地日志索引[{}]存在, 但任期[{}]与追加的日志任期[{}]不同", request.prevLogIndex(), localPrevLogTerm, request.prevLogTerm());
                    assert request.prevLogIndex() > localPrevLogTerm; // 分区后选举出来的新领导者一定比将要下线的老领导者任期大(由预投票保证)
                    lastLogIndex = logRepository.getLastLogId().index();
                    break;
                }
                if (request.logEntries().isEmpty()) {
                    LOG.info("收到心跳或探针请求, 返回最后的日志索引{}", lastLogIndex = logRepository.getLastLogId().index());
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
            return new AppendEntriesResponse(currTerm, success, lastLogIndex);
        } finally {
            lock.unlock();
        }
    }

    private void handleElectionTimeout(boolean preVote) {
        lock.lock();
        try {
            if (
                    state != State.FOLLOWER // 必须是跟随者才尝试变成候选者.
                    || isCurrentLeaderValid() // election_timeout_ms 内没有收到领导者的消息, 才尝试变成候选者(因为心跳等消息的时间间隔远小于 election_timeout_ms)
            ) {
                return;
            }
            long term;
            if (preVote) { // 正式投票时更改状态和当前任期的投票
                LOG.info("发起预投票, 当前任期:[{}]", term = currTerm + 1); // 预投票不改变任期, 防止对称分区任期不断增加
            } else {
                LOG.info("发起正式投票, 竞选任期:[{}]", term = ++currTerm); // 正式投票改变任期
                state = State.CANDIDATE;
                votedEndpoint = configuration.getEndpoint();
            }
            LongAdder voteCount = new LongAdder(); // 赞成票计数
            voteCount.increment();
            LogId lastLogId = logRepository.getLastLogId();
            RequestVoteRequest request = new RequestVoteRequest(
                    configuration.getEndpoint(),
                    term,
                    lastLogId.index(),
                    lastLogId.term(),
                    preVote
            );
            /*
            在发生网络分区时 RPC 超时会占用锁很长时间, 而此时可能收到来自竞选成功的领导者的消息, 此时可以直接转变为跟随者,
            这可以通过细化锁来优化, 但需要加锁的很多 try finally 代码块和 ABA 判断, 这是一个不太影响 Raft 算法, 但实际中需要考虑的问题
             */
            sendRequestAllOf(
                    endpoint -> {
                        RpcRequest rpcRequest = new RpcRequest(endpoint, request, configuration.getElectionTimeoutMs());
                        // 发起投票请求
                        if (rpcService.sendRequest(rpcRequest) instanceof RequestVoteResponse response) {
                            if (response.term() > term) { // 其他人的任期更高, 回退为跟随者
                                LOG.info("其他节点[{}]任期[{}]更高, 当前节点准备回退为跟随者. 当前节点任期:[{}]", endpoint, response.term(), term);
                                voteCount.add(-quorum());
                            } else if (response.granted()) {
                                LOG.info("其他节点[{}]赞同投票. 当前节点任期:[{}]", endpoint, term);
                                voteCount.increment();
                            } else {
                                LOG.info("其他节点[{}]任期小, 但日志更多, 不投票. 当前节点任期:[{}], ", endpoint, term);
                            }
                        }
                    },
                    (endpoint, throwable) -> {
                        LOG.info("请求另一个节点投票时出错, 可能存在网络分区. 节点:[{}]", endpoint);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("出错原因如下", throwable);
                        }
                    }
            );
            if (request.preVote()) { // 预投票结果判断
                if (voteCount.sum() >= quorum()) {
                    LOG.info("预投票票数[{}]超过一半节点, 开始正式投票", voteCount.sum());
                    lock.unlock();
                    handleElectionTimeout(false);
                } else {
                    LOG.info("预投票票数[{}]少于一半节点, 投票失败", voteCount.sum());
                }
            } else if (voteCount.sum() >= quorum()) {
                LOG.info("正式投票票数[{}]超过一半节点, 上升为领导者", voteCount.sum());
                becomeLeader();
            } else {
                LOG.info("正式投票票数[{}]少于一半节点, 下降为跟随者", voteCount.sum());
                state = State.FOLLOWER;
                votedEndpoint = null;
            }
        } finally {
            lock.unlock();
        }
    }

    private long quorum() {
        return configuration.getOtherEndpoints().size() / 2 + 1;
    }

    private void becomeLeader() {
        state = State.LEADER;
        votedEndpoint = null;
        electionTimer.stop();
        LongAdder voteCount = new LongAdder(); // 反对票计数
        sendRequestAllOf(
                endpoint -> {
                    long prevLogIndex = nextIndexes.get(endpoint) - 1;
                    long prevLogTerm = logRepository.getTerm(prevLogIndex);
                    AppendEntriesRequest request = new AppendEntriesRequest(currTerm, Collections.emptyList(), prevLogTerm, prevLogIndex);
                    RpcRequest rpcRequest = new RpcRequest(endpoint, request, configuration.getHeartbeatTimeoutMs());
                    if (rpcService.sendRequest(rpcRequest) instanceof AppendEntriesResponse response) {
                        if (response.success()) {
                            matchIndexes.put(endpoint, response.lastLogIndex());
                        } else if (response.term() > currTerm) {
                            LOG.info("探针发现节点[{}]任期[{}]大于当前节点任期[{}], 下降为跟随者", endpoint, response.term(), currTerm);
                            voteCount.add(quorum());
                        } else {
                            if (response.lastLogIndex() + 1 < prevLogTerm) { // 比当前节点小说明索引为 prevLogIndex 的日志不存在, 用它的最新日志继续探测
                                nextIndexes.put(endpoint, response.lastLogIndex() + 1);
                            } else {
                                nextIndexes.computeIfPresent(endpoint, (__, nextIndex) -> nextIndex - 1);
                                assert nextIndexes.get(endpoint) >= 0; // 绝对不可能小于 0
                            }
                            // TODO: 2023/11/1 继续探测 
                        }
                    }
                },
                (endpoint, throwable) -> {
                    LOG.info("探针请求另一个节点[{}]时出错, 可能存在网络分区", endpoint);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("出错原因如下", throwable);
                    }
                    voteCount.increment();
                    matchIndexes.put(endpoint, 0L);
                }
        );
        if (voteCount.sum() >= quorum()) { // 大多数节点不赞成或网络分区, 下降为跟随者
            LOG.info("探针判定当前节点从领导者下降为跟随者");
            state = State.FOLLOWER;
            votedEndpoint = null;
            electionTimer.start();
            return;
        }
        long nextIndex = logRepository.getLastLogId().index() + 1;
        configuration.getOtherEndpoints().forEach(endpoint -> nextIndexes.put(endpoint, nextIndex));
        heartbeatTimer.start();
    }

    private boolean isCurrentLeaderValid() {
        return System.currentTimeMillis() - lastLeaderTimestamp < configuration.getElectionTimeoutMs();
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
            sendRequestAllOf(
                    endpoint -> {
                        long prevLogIndex = nextIndexes.get(endpoint) - 1;
                        long prevLogTerm = logRepository.getTerm(prevLogIndex);
                        AppendEntriesRequest request = new AppendEntriesRequest(currTerm, Collections.emptyList(), prevLogTerm, prevLogIndex);
                        RpcRequest rpcRequest = new RpcRequest(endpoint, request, configuration.getHeartbeatTimeoutMs());
                        if (rpcService.sendRequest(rpcRequest) instanceof AppendEntriesResponse response) {
                            if (response.term() > currTerm) {
                                LOG.info("心跳发现节点[{}]任期[{}]大于当前节点任期[{}], 下降为跟随者", endpoint, response.term(), currTerm);
                                voteCount.add(quorum());
                            }
                        }
                    },
                    (endpoint, throwable) -> {
                        LOG.info("心跳请求另一个节点[{}]时出错, 可能存在网络分区", endpoint);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("出错原因如下", throwable);
                        }
                        voteCount.increment();
                    }
            );
            if (voteCount.sum() >= quorum()) { // 大多数节点不赞成或网络分区, 下降为跟随者
                LOG.info("心跳判定当前节点从领导者下降为跟随者");
                state = State.FOLLOWER;
                votedEndpoint = null;
                heartbeatTimer.stop();
                electionTimer.start();
            } else {
                LOG.info("心跳成功, 领导者任期续期");
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
