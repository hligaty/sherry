package io.github.hligaty.raft.storage;

import java.io.Closeable;
import java.util.List;

public interface LogRepository extends Closeable {
    
    void appendEntries(List<LogEntry> logEntries) throws StoreException;
    
    LogEntry get(long index);

    LogId getLastLogId();

    void truncateSuffix(final long lastIndexKept);
}
