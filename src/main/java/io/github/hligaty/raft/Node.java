package io.github.hligaty.raft;

import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.AppendEntriesResponse;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;

import java.io.Serializable;

public interface Node {
    
    void setConfiguration(Configuration configuration);
    
    void startup();

    <T extends Serializable> void apply(T data);
    
    RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest requestVoteRequest);
    
    AppendEntriesResponse handleAppendEntriesRequest(AppendEntriesRequest appendEntriesRequest);
}
