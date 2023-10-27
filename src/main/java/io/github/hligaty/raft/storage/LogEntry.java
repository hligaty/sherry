package io.github.hligaty.raft.storage;

import java.io.Serializable;

public record LogEntry(
        LogId logId,
        Object object
) implements Serializable {
}
