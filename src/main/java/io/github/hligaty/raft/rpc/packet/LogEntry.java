package io.github.hligaty.raft.rpc.packet;

import java.io.Serializable;

public record LogEntry(
        long term,
        long index,
        Object object
) implements Serializable {
}
