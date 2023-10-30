package io.github.hligaty.raft.config;

import io.github.hligaty.raft.util.Endpoint;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    /**
     * 当前节点地址
     */
    private Endpoint endpoint;

    /**
     * 其他节点地址
     */
    private final List<Endpoint> otherEndpoints = new ArrayList<>();

    /**
     * 选举超时时间, 超过这个时间且没有收到来自领导者的消息则变成候选者
     */
    private int electionTimeoutMs = 1000;
    
    /**
     * 最大随机选举延迟时间, 利用随机来避免死锁
     */
    private int maxElectionDelayMs = 1000;

    /**
     * RPC 客户端连接超时时间
     */
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
