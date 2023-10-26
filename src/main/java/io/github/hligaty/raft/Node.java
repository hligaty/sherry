package io.github.hligaty.raft;

import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;

public interface Node {
    
    void setConfiguration(Configuration configuration);
    
    void startup();
    
    RequestVoteResponse handleVoteRequest(RequestVoteRequest requestVoteRequest);
    
    boolean handleAppendEntries(AppendEntriesRequest appendEntriesRequest);
}
