package io.github.hligaty.raft.storage;

import io.github.hligaty.raft.config.Configuration;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.List;

public class RocksDBRepository implements LogRepository {

    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB rocksDB;

    public RocksDBRepository(Configuration configuration) {
        Options options = new Options().setCreateIfMissing(true);
        try {
            rocksDB = RocksDB.open(options, "./rocksdb-data-" + configuration.getEndpoint().port() + "/");
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    public void appendEntries(List<LogEntry> logEntries) {
        
    }

    @Override
    public LogEntry get(long index) {
        return null;
    }

    @Override
    public LogId getLastLogId() {
        return null;
    }

    @Override
    public void close() {
        rocksDB.close();
    }
}
