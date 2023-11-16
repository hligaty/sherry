package io.github.hligaty.raft.core;

public enum ErrorType {
    NOT_LEADER,
    REPLICATION_FAIL,
    READ_INDEX_READ_FAIL,
    STATE_MACHINE_FAIL,
}
