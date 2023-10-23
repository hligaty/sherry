package io.github.hligaty.raft.standard.config;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private Endpoint endpoint;
    
    private final List<Endpoint> peerNodes = new ArrayList<>();
    
    private int electionTimeoutMs = 1000;
    
    private int maxElectionDelayMs = 1000;
    
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Configuration setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public List<Endpoint> getPeerNodes() {
        return peerNodes;
    }

    public Configuration addPeerNodes(List<Endpoint> endpoints) {
        peerNodes.addAll(endpoints);
        return this;
    }

    public Configuration addPeerNode(Endpoint endpoint) {
        peerNodes.add(endpoint);
        return this;
    }

    public int getElectionTimeoutMs() {
        return electionTimeoutMs;
    }

    public void setElectionTimeoutMs(int electionTimeoutMs) {
        this.electionTimeoutMs = electionTimeoutMs;
    }

    public int getHeartbeatTimeoutMs() {
        return electionTimeoutMs / 10;
    }

    public int getMaxElectionDelayMs() {
        return maxElectionDelayMs;
    }

    public void setMaxElectionDelayMs(int maxElectionDelayMs) {
        this.maxElectionDelayMs = maxElectionDelayMs;
    }
}
