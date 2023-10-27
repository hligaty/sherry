package io.github.hligaty.raft.storage;

import java.io.Closeable;
import java.util.List;

public interface LogRepository extends Closeable {
    
    boolean appendEntries(List<LogEntry> logEntries) throws StoreException;
    
    LogEntry get(long index);

    LogId getLastLogId();
    
}
