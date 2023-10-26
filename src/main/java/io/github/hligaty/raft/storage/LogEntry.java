package io.github.hligaty.raft.storage;

import java.io.Serializable;

public record LogEntry(
        LogId logId
) implements Serializable {
}
