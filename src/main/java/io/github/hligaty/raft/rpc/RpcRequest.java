package io.github.hligaty.raft.rpc;

import io.github.hligaty.raft.rpc.packet.PeerId;

import java.io.Serializable;

public record RpcRequest(
        PeerId remoteId,
        Object request,
        int timeoutMillis
) implements Serializable {

}
