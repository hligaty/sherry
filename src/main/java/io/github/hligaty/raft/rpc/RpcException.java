package io.github.hligaty.raft.rpc;

public class RpcException extends RuntimeException {
    public RpcException(Throwable cause) {
        super(cause);
    }
}
