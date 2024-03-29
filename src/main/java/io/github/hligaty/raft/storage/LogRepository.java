package io.github.hligaty.raft.storage;

import io.github.hligaty.raft.rpc.packet.LogEntry;
import io.github.hligaty.raft.rpc.packet.LogId;

import java.io.Closeable;
import java.io.Serializable;
import java.util.List;

public interface LogRepository extends Closeable {
    
    LogEntry appendEntry(long term, Serializable data);
    
    void appendEntries(List<LogEntry> logEntries);
    
    LogEntry getEntry(long index);
    
    long getLastLogIndex();
    
    LogId getLastLogId();
    
    List<LogEntry> getSuffix(long beginIndex);

    List<LogEntry> getSuffix(long beginIndex, long endIndex);

    void truncateSuffix(long lastIndexKept);
    
}
