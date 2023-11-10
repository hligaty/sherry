package io.github.hligaty.raft.core;

import io.github.hligaty.raft.Node;
import io.github.hligaty.raft.StateMachine;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.rpc.RpcException;
import io.github.hligaty.raft.rpc.RpcRequest;
import io.github.hligaty.raft.rpc.RpcService;
import io.github.hligaty.raft.rpc.SofaBoltService;
import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.AppendEntriesResponse;
import io.github.hligaty.raft.rpc.packet.Command;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;
import io.github.hligaty.raft.storage.LogEntry;
import io.github.hligaty.raft.storage.LogId;
import io.github.hligaty.raft.storage.LogRepository;
import io.github.hligaty.raft.storage.StoreException;
import io.github.hligaty.raft.storage.impl.LocalRaftMetaRepository;
import io.github.hligaty.raft.storage.impl.RocksDBLogRepository;
import io.github.hligaty.raft.rpc.packet.PeerId;
import io.github.hligaty.raft.util.RepeatedTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class DefaultNode implements Node, RaftServerService {

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

    /**
     * Raft 元数据存储
     */
    private LocalRaftMetaRepository raftMetaRepository;

    private final StateMachine stateMachine;

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    /**
     * 全局锁, 控制 Raft 需要的状态等数据的变动. 两条线内的数据需要通过锁变更, 并且需要重启后也能恢复(状态就不需要了, 重启后肯定是跟随者)
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
    private PeerId votedId;

    /**
     * 当前节点为领导者时使用, 记录最后已提交到状态机的日志索引
     */
    private long committedIndex;

    //↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑

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

    public DefaultNode(StateMachine stateMachine) {
        this.lock = new ReentrantLock();
        this.virtualThreadPerTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.stateMachine = stateMachine;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void startup() {
        LOG.info("节点启动!!!");
        try {
            Files.createDirectories(configuration.getDataPath());
        } catch (IOException e) {
            throw new StoreException(e);
        }
        state = State.FOLLOWER;
        raftMetaRepository = new LocalRaftMetaRepository(configuration.getDataPath());
        currTerm = raftMetaRepository.getTerm();
        votedId = raftMetaRepository.getVotedFor();
        committedIndex = raftMetaRepository.getCommittedIndex();
        logRepository = new RocksDBLogRepository(configuration.getDataPath());
        lastLeaderTimestamp = System.currentTimeMillis();
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
        rpcService = new SofaBoltService(configuration, this);
        electionTimer.start();
    }

    @Override
    public void shutdown() {
        rpcService.shutdown();
        try {
            logRepository.close();
        } catch (IOException e) {
            throw new StoreException(e);
        }
        LOG.info("节点关闭!!!");
    }

    @Override
    public <R extends Serializable> R apply(Command command) {
        lock.lock();
        try {
            if (state != State.LEADER) {
                throw new ApplyException(ErrorType.NOT_LEADER);
            }
            LogEntry logEntry = logRepository.appendEntry(currTerm, command);
            if (!sendEntries()) {
                // 客户端收到这个错误不代表就执行失败了, 之后还可能复制到大多数(可能是当前节点或复制成功的少数节点当选领导者后复制的)节点
                throw new ApplyException(ErrorType.REPLICATION_FAIL);
            }
            List<LogEntry> logEntries = logEntry.logId().index() - committedIndex > 1
                    ? logRepository.getSuffix(committedIndex, logEntry.logId().index())
                    : Collections.singletonList(logEntry);
            return logEntries.stream()
                    .reduce(null, (__, entry) -> doApply(entry), (__, result) -> result);
        } finally {
            lock.unlock();
        }
    }

    private <R extends Serializable> R doApply(LogEntry logEntry) {
        R result = stateMachine.apply(logEntry.command());
        committedIndex = logEntry.logId().index();
        /*
        需要保证状态机执行命令和保存 committedIndex 是原子性的,
        当然如果状态机满足幂等的话也就不需要持久化 committedIndex, 反正重放不影响状态机的状态,
        比如 KV 数据库的 get/set/delete 执行多少次都没关系, 但计数器这种多加一次都不行
         */
        raftMetaRepository.saveCommittedIndex(committedIndex);
        LOG.info("更新应用到状态机的日志索引为[{}]", committedIndex);
        return result;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest request) {
        lock.lock();
        try {
            boolean granted = false;
            do {
                if (request.term() < currTerm) { // 比自己的任期小, 拒绝
                    LOG.info("收到投票请求, 拒绝. 但任期[{}]比当前节点任期[{}]小", request.term(), currTerm);
                    break;
                }
                if (request.preVote() && isCurrentLeaderValid()) { // 预投票阶段领导者有效就拒绝. 正式投票时不判断, 因为预投票通过表示大部分节点都同意了
                    LOG.info("收到预投票请求, 拒绝. 但领导者有效");
                    break;
                }
                if (!request.preVote() && request.term() > currTerm) {
                    LOG.info("正式投票请求的任期[{}]比当前节点任期[{}]大, 更改当前节点状态为跟随者", request.term(), currTerm);
                    stepDown(request.term());
                }
                if (!request.preVote() && !votedId.isEmpty()) { // 正式投票时判断是否已经投过了. 预投票只是为了判断是否可以发起正式投票, 不用记录投了谁
                    LOG.info("收到正式投票请求, 拒绝. 任期[{}]的正式投票已经投给了[{}]", currTerm, votedId);
                    assert state == State.FOLLOWER;
                    break;
                }
                /*
                 * 候选者日志比当前节点日志少, 拒绝
                 * 这个是 Raft 的核心思想, 只有大多数节点复制日志成功才算成功, 因此, 基于该条件的投票机制也就能保证收到大多数票的那个节点有最新的日志
                 */
                LogId localLastLogId = logRepository.getLastLogId();
                if (new LogId(request.lastLogTerm(), request.lastLogIndex()).compareTo(localLastLogId) < 0) {
                    LOG.info("收到投票请求, 拒绝. 请求的日志[{}]比本地日志[{}]少", new LogId(request.lastLogTerm(), request.lastLogIndex()), localLastLogId);
                    break;
                }
                LOG.info("收到投票请求, 同意. 当前和对方任期[{}] [{}], 当前和对方最后日志索引[{}] [{}]",
                        currTerm, request.term(), localLastLogId, new LogId(request.lastLogTerm(), request.lastLogIndex()));
                granted = true;
                if (!request.preVote()) { // 正式投票阶段投票后更改状态为跟随者
                    stepDown(request.term());
                    votedId = request.peer();
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
                if (request.term() > currTerm) {
                    LOG.info("收到新领导任期[{}]的追加日志请求, 更改当前状态[{}]和任期[{}]", request.term(), state, currTerm);
                    stepDown(request.term());
                }
                /*
                解决这个问题: 网络分区后, 旧领导者的日志复制了少部分节点后才意识到不是领导者了(但日志已经过去了), 然后变成跟随者, 这个日志需要删除
                但这部分日志只能由新领导者与分区的节点协商, 找到共同的日志 A 后删除日志 A 后面的日志, 并复制新领导者日志 A 后面的日志来解决
                 */
                long localPrevLogTerm = Optional.ofNullable(logRepository.getEntry(request.prevLogIndex()))
                        .map(logEntry -> logEntry.logId().term())
                        .orElse(0L);
                if (request.prevLogTerm() != localPrevLogTerm) {
                    lastLogIndex = logRepository.getLastLogIndex();
                    LOG.info("本地日志索引[{}]的任期[{}]与追加的日志任期[{}]不同, 返回当前最大日志索引{}",
                            request.prevLogIndex(), localPrevLogTerm, request.prevLogTerm(), lastLogIndex);
                    break;
                }
                if (request.logEntries().isEmpty()) {
                    LOG.info("收到心跳或探测请求, 且与预期的前一个日志的索引[{}]和任期[{}]匹配, 任期[{}]", request.prevLogIndex(), request.prevLogTerm(), currTerm);
                    success = true;
                    // 也返回最后的日志索引, 这样(response.lastLogIndex() + 1 < prevLogTerm 为 false)领导者就可以从后向前匹配了
                    lastLogIndex = logRepository.getLastLogIndex();
                    long commitIndex = Math.min(request.committedIndex(), lastLogIndex);
                    LOG.info("将索引{}和{}之间的日志应用到状态机", committedIndex + 1, commitIndex);
                    logRepository.getSuffix(committedIndex + 1, commitIndex).forEach(this::doApply);
                    break;
                }
                try {
                    LOG.info("删除索引[{}]之后的日志", request.prevLogIndex());
                    logRepository.truncateSuffix(request.prevLogIndex());
                    LOG.info("添加索引[{}]之后的日志[{}]", request.prevLogIndex(), request.logEntries());
                    logRepository.appendEntries(request.logEntries());
                    success = true;
                } catch (Exception e) {
                    LOG.error("追加日志失败!!! 日志内容:[{}]", request.logEntries(), e);
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
            LogId lastLogId = logRepository.getLastLogId();
            if (preVote) { // 正式投票时更改状态和当前任期的投票
                LOG.info("发起预投票, 预选任期:[{}], 最后的日志索引[{}]", term = currTerm + 1, lastLogId); // 预投票不改变任期, 防止对称分区任期不断增加
            } else {
                LOG.info("发起正式投票, 竞选任期:[{}], 最后的日志索引[{}]", term = ++currTerm, lastLogId); // 正式投票改变任期
                state = State.CANDIDATE;
                votedId = configuration.getPeer().id();
            }
            LongAdder voteCount = new LongAdder(); // 赞成票计数
            voteCount.increment();
            RequestVoteRequest request = new RequestVoteRequest(
                    configuration.getPeer().id(),
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
                    peer -> {
                        RpcRequest rpcRequest = new RpcRequest(peer.id(), request, configuration.getElectionTimeoutMs());
                        // 发起投票请求
                        RequestVoteResponse response;
                        try {
                            response = (RequestVoteResponse) rpcService.sendRequest(rpcRequest);
                        } catch (Exception e) {
                            LOG.info("节点:[{}]投票结果, 拒绝. 可能存在网络分区. 当前节点任期[{}]", peer.id(), currTerm);
                            if (LOG.isDebugEnabled() || !(e instanceof RpcException)) {
                                LOG.error("出错原因如下", e);
                            }
                            return;
                        }
                        if (response.term() > term) {
                            LOG.info("节点:[{}]投票结果, 拒绝. 任期[{}]更高. 当前节点任期:[{}]", peer.id(), response.term(), currTerm);
                            stepDown(response.term());
                        } else if (response.granted()) {
                            LOG.info("节点:[{}]投票结果, 赞同. 当前节点任期:[{}]", peer.id(), currTerm);
                            voteCount.increment();
                        } else {
                            LOG.info("节点:[{}]投票结果, 拒绝. 对方任期[{}]不大, 但本地日志索引[{}]少. 当前节点任期:[{}], ", peer.id(), response.term(), request.lastLogIndex(), currTerm);
                        }
                    }
            );
            if (request.preVote()) { // 预投票结果判断
                if (voteCount.sum() >= configuration.quorum()) {
                    LOG.info("预投票票数[{}]超过一半节点, 开始正式投票", voteCount.sum());
                    handleElectionTimeout(false);
                } else {
                    LOG.info("预投票票数[{}]少于一半节点, 投票失败", voteCount.sum());
                }
            } else { // 正式投票结果判断
                if (voteCount.sum() >= configuration.quorum()) {
                    LOG.info("正式投票票数[{}]超过一半节点, 上升为领导者", voteCount.sum());
                    becomeLeader();
                } else {
                    LOG.info("正式投票票数[{}]少于一半节点, 下降为跟随者", voteCount.sum());
                    stepDown(currTerm);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void becomeLeader() {
        raftMetaRepository.setTermAndVotedFor(currTerm, configuration.getPeer().id());
        state = State.LEADER;
        votedId = PeerId.emptyId();
        electionTimer.stop();
        long nextIndex = logRepository.getLastLogIndex() + 1;
        configuration.getPeers().forEach(peer -> peer.setNextIndex(nextIndex));
        try {
            LOG.info("成为领导者后开始第一次追加日志");
            apply(new Command(null));
        } catch (ApplyException e) {
            LOG.info("成为领导者后第一次追加日志失败, 已下降为跟随者, 当前任期[{}]", currTerm);
            return;
        }
        heartbeatTimer.start();
    }

    private void sendProbeRequest(Peer peer) {
        if (state != State.LEADER) {
            LOG.info("探测节点[{}]时发现当前节点任期[{}]已不是领导者, 结束", peer.id(), currTerm);
            return;
        }
        long prevLogIndex = peer.nextIndex() - 1;
        long prevLogTerm = logRepository.getEntry(prevLogIndex).logId().term();
        AppendEntriesRequest request = new AppendEntriesRequest(currTerm, Collections.emptyList(), prevLogTerm, prevLogIndex, committedIndex);
        RpcRequest rpcRequest = new RpcRequest(peer.id(), request, configuration.getHeartbeatTimeoutMs());
        AppendEntriesResponse response = (AppendEntriesResponse) rpcService.sendRequest(rpcRequest);
        if (response.success()) {
            LOG.info("探测节点[{}]成功, 当前节点任期[{}], 将要发送的下一条日志索引[{}]", peer.id(), currTerm, prevLogIndex + 1);
        } else if (response.term() > currTerm) {
            LOG.info("探测发现节点[{}]任期[{}]大于当前节点任期[{}], 下降为跟随者", peer.id(), response.term(), currTerm);
            stepDown(response.term());
        } else {
            if (response.lastLogIndex() + 1 < prevLogIndex) { // 比当前节点小说明索引为 prevLogIndex 的日志不存在, 用它的最新日志继续探测
                LOG.info("探测发现预期的节点[{}]前一个日志[{}]不匹配, 且它最后一个日志索引[{}]更小, 用它最后一个日志再探测, 当前节点任期[{}]",
                        peer.id(), prevLogIndex, response.lastLogIndex(), currTerm);
                peer.setNextIndex(response.lastLogIndex() + 1);
            } else {
                LOG.info("探测发现预期的节点[{}]前一个日志[{}]不匹配, 且它最后一个日志索引[{}]更大, 向前再探测, 当前节点任期[{}]",
                        peer.id(), request.prevLogIndex(), response.lastLogIndex(), currTerm);
                peer.setNextIndex(peer.nextIndex() - 1);
                assert peer.nextIndex() >= 0; // 绝对不可能小于 0
            }
            LOG.info("再次探测节点[{}], 任期[{}]", peer.id(), currTerm);
            sendProbeRequest(peer);
        }
    }

    private boolean sendEntries() {
        LongAdder voteCount = new LongAdder(); // 赞同票计数
        voteCount.increment();
        sendRequestAllOf(
                peer -> {
                    try {
                        sendEntries(peer);
                        voteCount.add(state == State.LEADER ? 1 : 0);
                    } catch (Exception e) {
                        LOG.info("节点[{}]追加日志时发生错误, 可能出现网络分区, 当前节点任期[{}]", peer.id(), currTerm);
                        if (LOG.isDebugEnabled() || !(e instanceof RpcException)) {
                            LOG.error("出错原因如下", e);
                        }
                    }
                }
        );
        if (voteCount.sum() < configuration.quorum()) {
            LOG.info("追加日志不被大多数节点认可, 可能是网络分区, 下降为跟随者");
            stepDown(currTerm);
            return false;
        }
        return state == State.LEADER;
    }

    private void sendEntries(Peer peer) {
        if (state != State.LEADER) {
            LOG.info("其他节点[{}]追加日志时发现当前节点任期[{}]已不是领导者, 结束", peer.id(), currTerm);
            return;
        }
        long prevLogIndex = peer.nextIndex() - 1;
        LogEntry entry = logRepository.getEntry(prevLogIndex);
        long prevLogTerm = entry.logId().term();
        LOG.info("发送追加日志请求, 节点[{}]日志从索引[{}]开始复制, 已应用到状态机的日志索引[{}]", peer.id(), prevLogIndex + 1, committedIndex);
        List<LogEntry> logEntries = logRepository.getSuffix(prevLogIndex + 1);
        AppendEntriesRequest request = new AppendEntriesRequest(currTerm, logEntries, prevLogTerm, prevLogIndex, committedIndex);
        RpcRequest rpcRequest = new RpcRequest(peer.id(), request, configuration.getHeartbeatTimeoutMs());
        AppendEntriesResponse response = (AppendEntriesResponse) rpcService.sendRequest(rpcRequest);
        if (response.success()) {
            if (!logEntries.isEmpty()) {
                long newNextIndex = logEntries.getLast().logId().index() + 1;
                LOG.info("追加日志请求成功, 更新节点[{}]下一次开始发送的日志索引为[{}]", peer.id(), newNextIndex);
                peer.setNextIndex(newNextIndex);
            }
            return;
        }
        if (response.term() > currTerm) {
            LOG.info("其他节点[{}]任期[{}]追加日志时, 发现当前节点任期[{}]小, 下降为跟随者", peer.id(), response.term(), currTerm);
            stepDown(response.term());
        } else {
            LOG.info("其他节点[{}]任期[{}]追加日志失败, 发送探测请求后重新追加, 当前节点任期[{}]", peer.id(), response.term(), currTerm);
            sendProbeRequest(peer);
            sendEntries(peer);
        }
    }

    private final Lock termLock = new ReentrantLock();

    private void stepDown(long newTerm) {
        state = State.FOLLOWER;
        votedId = PeerId.emptyId();
        termLock.lock();
        try {
            if (newTerm > currTerm) {
                currTerm = newTerm;
                raftMetaRepository.setTermAndVotedFor(currTerm, votedId);
            }
        } finally {
            termLock.unlock();
        }
        heartbeatTimer.stop();
        lastLeaderTimestamp = System.currentTimeMillis();
        electionTimer.start();
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
            LOG.info("发送心跳, 当前任期[{}]", currTerm);
            if (sendEntries()) {
                LOG.info("心跳成功, 领导者任期[{}]续期", currTerm);
            } else {
                LOG.info("心跳失败, 判定当前节点任期[{}]从领导者下降为跟随者", currTerm);
            }
        } finally {
            lock.unlock();
        }
    }

    private void sendRequestAllOf(Consumer<Peer> requestHandler) {
        CompletableFuture
                .allOf(
                        configuration.getPeers().stream()
                                .map(peer -> CompletableFuture.runAsync(() -> requestHandler.accept(peer), virtualThreadPerTaskExecutor))
                                .toArray(CompletableFuture[]::new))
                .join();
    }
}
