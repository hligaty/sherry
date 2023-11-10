package io.github.hligaty.raft.core;

import java.io.Serializable;

public enum ErrorType implements Serializable {
    NOT_LEADER,
    REPLICATION_FAIL
}
