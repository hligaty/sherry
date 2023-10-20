package io.github.hligaty.raft.standard.rpc;

public interface RpcService {
    
    Response handleRequest(Request request);
}
