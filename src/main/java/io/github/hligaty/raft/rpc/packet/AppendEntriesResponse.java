package io.github.hligaty.raft.rpc.packet;

import java.io.Serializable;

public record AppendEntriesResponse(
        long term,
        boolean success
) implements Serializable {
}
