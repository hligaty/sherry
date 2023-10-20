package io.github.hligaty.raft.standard;

import io.github.hligaty.raft.standard.storage.LogEntry;

public class DefaultNode implements Node {
    
    @Override
    public void setConfig(Config config) {

    }

    @Override
    public boolean voteFor(PeerNode peerNode) {
        return false;
    }

    @Override
    public void appendEntry(LogEntry logEntry) {
        
    }
}
