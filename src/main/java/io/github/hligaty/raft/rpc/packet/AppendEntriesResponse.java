package io.github.hligaty.raft.rpc.packet;

import java.io.Serializable;

public record AppendEntriesResponse(
        long term
) implements Serializable {
}
