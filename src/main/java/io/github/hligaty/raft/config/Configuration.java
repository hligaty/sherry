package io.github.hligaty.raft.config;

import io.github.hligaty.raft.util.Peer;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    /**
     * 当前节点地址
     */
    private Peer peer;

    /**
     * 其他节点地址
     */
    private final List<Peer> peers = new ArrayList<>();

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
    
    public Peer getPeer() {
        return peer;
    }

    public Configuration setPeer(Peer peer) {
        this.peer = peer;
        return this;
    }

    public List<Peer> getPeers() {
        return peers;
    }

    public Configuration addPeerNodes(List<Peer> peers) {
        this.peers.addAll(peers);
        return this;
    }

    public Configuration addPeerNode(Peer peer) {
        peers.add(peer);
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
