package io.github.hligaty.raft.rpc;

public interface RpcService {

    Object handleRequest(Object request);

    Object sendRequest(RpcRequest rpcRequest) throws RpcException;
    
    void shutdown();
}
