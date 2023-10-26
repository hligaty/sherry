package io.github.hligaty.raft.rpc.packet;

import io.github.hligaty.raft.storage.LogEntry;

import java.io.Serializable;
import java.util.List;

public record AppendEntriesRequest(
        long term,
        List<LogEntry> logEntries
) implements Serializable {
}
