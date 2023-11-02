package io.github.hligaty.raft.storage;

import io.github.hligaty.raft.config.Configuration;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.LongStream;

public class RocksDBRepository implements LogRepository {

    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB rocksDB;

    private final ReentrantLock lock = new ReentrantLock();

    public RocksDBRepository(Configuration configuration) {
        Options options = new Options().setCreateIfMissing(true);
        try {
            rocksDB = RocksDB.open(options, "./rocksdb-data-" + configuration.getPeer().port() + "/");
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    public void appendEntries(final List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            int size = logEntries.size();
            long count = logEntries.stream().filter(logEntry -> logEntry.logId() != null).count();
            List<LogEntry> list;
            if (count == 0) {
                long lastLogIndex = getLastLogId().index();
                list = LongStream.range(0, size).mapToObj(i -> logEntries.get((int) i).setLogIndex(lastLogIndex + i + 1)).toList();
            } else if (count == size) {
                list = logEntries;
            } else {
                throw new StoreException("Inconsistent logEntries");
            }
            // TODO: 2023/11/2 保存 
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogEntry get(long index) {
        lock.lock();
        try {
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogId getLastLogId() {
        lock.lock();
        try {
            return new LogId(0, 0);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void truncateSuffix(long lastIndexKept) {
        lock.lock();
        try {
            
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        rocksDB.close();
    }
}
