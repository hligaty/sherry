package io.github.hligaty.raft.rpc;

import io.github.hligaty.raft.util.Endpoint;

import java.io.Serializable;

public record RpcRequest(
        Endpoint endpoint,
        Object request,
        int timeoutMillis
) implements Serializable {

}
