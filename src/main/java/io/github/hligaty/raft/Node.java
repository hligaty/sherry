package io.github.hligaty.raft;

import io.github.hligaty.raft.config.Configuration;

public interface Node {
    
    void setConfiguration(Configuration configuration);
    
    void startup();
    
    void shutdown();
}
