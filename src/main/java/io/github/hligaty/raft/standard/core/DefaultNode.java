package io.github.hligaty.raft.standard.core;

import io.github.hligaty.raft.standard.Node;
import io.github.hligaty.raft.standard.config.Configuration;
import io.github.hligaty.raft.standard.config.Endpoint;
import io.github.hligaty.raft.standard.rpc.RpcService;
import io.github.hligaty.raft.standard.rpc.SofaBoltService;
import io.github.hligaty.raft.standard.rpc.packet.LogEntryReq;
import io.github.hligaty.raft.standard.rpc.packet.VoteReq;
import io.github.hligaty.raft.standard.util.RepeatedTimer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultNode implements Node {

    private Configuration configuration;

    private Endpoint endpoint;

    private RpcService rpcService;

    private volatile State state;

    private AtomicLong currTerm;

    private volatile long lastLeaderTimestamp;

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        endpoint = configuration.getEndpoint();
    }

    @Override
    public void start() {
        state = State.FOLLOWER;
        rpcService = new SofaBoltService(endpoint.port(), this);
        RepeatedTimer heartbeatTimeoutTimer = new RepeatedTimer("heartbeatTimeoutTimer") {

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
        heartbeatTimeoutTimer.start();
    }

    @Override
    public boolean voteFor(VoteReq voteReq) {
        return false;
    }

    @Override
    public boolean appendEntry(LogEntryReq logEntryReq) {
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
        
    }
}
