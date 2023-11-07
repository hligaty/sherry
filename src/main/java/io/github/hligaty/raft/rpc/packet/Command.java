package io.github.hligaty.raft.rpc.packet;

import java.io.Serializable;

public class Command implements Serializable {
    
    private final Serializable data;

    public Command(Serializable data) {
        this.data = data;
    }

    public Serializable getData() {
        return data;
    }
}
