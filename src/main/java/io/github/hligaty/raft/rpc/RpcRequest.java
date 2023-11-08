package io.github.hligaty.raft.rpc;

import io.github.hligaty.raft.util.PeerId;

import java.io.Serializable;

public record RpcRequest(
        PeerId peerId,
        Object request,
        int timeoutMillis
) implements Serializable {

}
