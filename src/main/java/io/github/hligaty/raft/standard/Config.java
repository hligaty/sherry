package io.github.hligaty.raft.standard;

import java.util.List;

public class Config {
    
    int port;
    
    List<Integer> peerNodePorts;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<Integer> getPeerNodePorts() {
        return peerNodePorts;
    }

    public void setPeerNodePorts(List<Integer> peerNodePorts) {
        this.peerNodePorts = peerNodePorts;
    }
}
