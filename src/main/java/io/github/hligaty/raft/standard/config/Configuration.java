package io.github.hligaty.raft.standard.config;

import io.github.hligaty.raft.standard.util.Endpoint;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private Endpoint endpoint;
    
    private final List<Endpoint> otherEndpoints = new ArrayList<>();
    
    private int electionTimeoutMs = 1000;
    
    private int maxElectionDelayMs = 1000;
    
    private int rpcConnectTimeoutMs = 1000;
    
    private int rpcRequestTimeoutMs = 5000;
    
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Configuration setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public List<Endpoint> getOtherEndpoints() {
        return otherEndpoints;
    }

    public Configuration addPeerNodes(List<Endpoint> endpoints) {
        this.otherEndpoints.addAll(endpoints);
        return this;
    }

    public Configuration addPeerNode(Endpoint endpoint) {
        otherEndpoints.add(endpoint);
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

    public int getRpcConnectTimeoutMs() {
        return rpcConnectTimeoutMs;
    }

    public void setRpcConnectTimeoutMs(int rpcConnectTimeoutMs) {
        this.rpcConnectTimeoutMs = rpcConnectTimeoutMs;
    }

    public int getRpcRequestTimeoutMs() {
        return rpcRequestTimeoutMs;
    }

    public void setRpcRequestTimeoutMs(int rpcRequestTimeoutMs) {
        this.rpcRequestTimeoutMs = rpcRequestTimeoutMs;
    }
}
