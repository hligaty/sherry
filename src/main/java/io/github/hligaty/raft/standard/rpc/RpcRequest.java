package io.github.hligaty.raft.standard.rpc;

import io.github.hligaty.raft.standard.util.Endpoint;

import java.io.Serializable;

public record RpcRequest(
        Endpoint endpoint,
        Object request,
        int timeoutMillis
) implements Serializable {

}
