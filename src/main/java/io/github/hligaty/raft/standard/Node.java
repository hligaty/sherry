package io.github.hligaty.raft.standard;

import io.github.hligaty.raft.standard.storage.LogEntry;

public interface Node {
    
    void setConfig(Config config);
    
    boolean voteFor(PeerNode peerNode);
    
    void appendEntry(LogEntry logEntry);
    
}
