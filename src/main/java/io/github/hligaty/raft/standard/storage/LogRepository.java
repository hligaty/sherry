package io.github.hligaty.raft.standard.storage;

import java.io.Closeable;

public interface LogRepository extends Closeable {
    
    boolean append(LogEntry logEntry);
    
    LogEntry get(long index);

    LogId getLastLogId();
    
}
