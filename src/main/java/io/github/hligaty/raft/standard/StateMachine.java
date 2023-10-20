package io.github.hligaty.raft.standard;

import io.github.hligaty.raft.standard.storage.LogEntry;

public interface StateMachine {

    void apply(LogEntry logEntry);

}
