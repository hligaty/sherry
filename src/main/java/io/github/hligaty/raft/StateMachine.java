package io.github.hligaty.raft;

import io.github.hligaty.raft.storage.LogEntry;

public interface StateMachine {

    void apply(LogEntry logEntry);

}
