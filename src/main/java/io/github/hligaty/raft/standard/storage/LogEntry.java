package io.github.hligaty.raft.standard.storage;

import java.io.Serializable;

public record LogEntry(
        LogId logId
) implements Serializable {
}
