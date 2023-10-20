package io.github.hligaty.raft.standard.storage;

public interface LogRepository {
    
    boolean append(LogEntry logEntry);
    
    LogEntry get(long index);
    
}
