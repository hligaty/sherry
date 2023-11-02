package io.github.hligaty.raft.rpc;

import io.github.hligaty.raft.util.Peer;

import java.io.Serializable;

public record RpcRequest(
        Peer peer,
        Object request,
        int timeoutMillis
) implements Serializable {

}
