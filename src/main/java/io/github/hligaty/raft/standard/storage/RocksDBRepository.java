package io.github.hligaty.raft.standard.storage;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBRepository implements LogRepository {

    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB rocksDB;

    public RocksDBRepository() {
        Options options = new Options().setCreateIfMissing(true);
        try {
            rocksDB = RocksDB.open(options, "./rocksdb-data/");
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public boolean append(LogEntry logEntry) {
        return false;
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
