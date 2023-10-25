package io.github.hligaty.raft.standard.rpc.packet;

import java.io.Serializable;

public record RequestVoteResponse(
        long term,
        boolean granted
) implements Serializable {
}
