package io.github.hligaty.raft;

import io.github.hligaty.raft.config.Configuration;

import java.io.Serializable;

public interface Node {
    
    void setConfiguration(Configuration configuration);
    
    void startup();

    <T extends Serializable, R extends Serializable> R apply(T data);
}
