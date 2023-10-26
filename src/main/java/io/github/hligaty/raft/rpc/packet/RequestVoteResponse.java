package io.github.hligaty.raft.rpc.packet;

import java.io.Serializable;

public record RequestVoteResponse(
        long term,
        boolean granted
) implements Serializable {
}
