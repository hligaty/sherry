package io.github.hligaty.raft.standard.rpc;

public class RpcException extends RuntimeException {
    public RpcException(Throwable cause) {
        super(cause);
    }
}
