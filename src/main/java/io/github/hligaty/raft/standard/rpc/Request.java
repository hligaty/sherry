package io.github.hligaty.raft.standard.rpc;

import java.io.Serializable;

public record Request(
        PeerNode peerNode,
        Object data
) implements Serializable {

}
