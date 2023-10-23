package io.github.hligaty.raft.standard.rpc;

import java.io.Serializable;

public record PeerNode(
        String address,
        int port,
        long term,
        long logIndex
) implements Serializable {

}
