package io.github.hligaty.raft.standard;

import io.github.hligaty.raft.standard.config.Configuration;
import io.github.hligaty.raft.standard.rpc.packet.LogEntryReq;
import io.github.hligaty.raft.standard.rpc.packet.VoteReq;

public interface Node {
    
    void setConfiguration(Configuration configuration);
    
    void start();
    
    boolean voteFor(VoteReq voteReq);
    
    boolean appendEntry(LogEntryReq logEntryReq);
}
