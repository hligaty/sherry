package io.github.hligaty.raft.storage;

import java.io.Closeable;
import java.util.List;

public interface LogRepository extends Closeable {
    
    long appendEntry(LogEntry logEntry);
    
    void appendEntries(List<LogEntry> logEntries) throws StoreException;
    
    LogEntry getEntry(long index);
    
    long getLastLogIndex();
    
    LogId getLastLogId();
    
    List<LogEntry> getSuffix(long beginIndex);

    List<LogEntry> getSuffix(long beginIndex, long endIndex);

    void truncateSuffix(long lastIndexKept);
}
