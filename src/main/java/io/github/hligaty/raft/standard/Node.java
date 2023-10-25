package io.github.hligaty.raft.standard;

import io.github.hligaty.raft.standard.config.Configuration;
import io.github.hligaty.raft.standard.rpc.packet.AppendEntryRequest;
import io.github.hligaty.raft.standard.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.standard.rpc.packet.RequestVoteResponse;

public interface Node {
    
    void setConfiguration(Configuration configuration);
    
    void start();
    
    RequestVoteResponse handleVoteRequest(RequestVoteRequest requestVoteRequest);
    
    boolean appendEntry(AppendEntryRequest appendEntryRequest);
}
