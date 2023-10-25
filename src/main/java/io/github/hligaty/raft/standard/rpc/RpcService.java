package io.github.hligaty.raft.standard.rpc;

public interface RpcService {

    Object handleRequest(Object request);

    Object sendRequest(RpcRequest rpcRequest) throws RpcException;
}
