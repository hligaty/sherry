package io.github.hligaty.raft.infrastructure;

import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.storage.LogEntry;
import io.github.hligaty.raft.storage.LogId;
import io.github.hligaty.raft.storage.LogRepository;
import io.github.hligaty.raft.storage.RocksDBRepository;
import io.github.hligaty.raft.util.Peer;
import org.junit.jupiter.api.Test;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class StoreTest {
    
    static {
        RocksDB.loadLibrary();
    }

    @Test
    public void test() throws RocksDBException {
        try (final Options options = new Options().setCreateIfMissing(true)) {
            byte[] key = "hello".getBytes();
            byte[] expected = "world".getBytes();
            try (final RocksDB rocksDB = RocksDB.open(options, "./rocksdb-data/")) {
                rocksDB.put(key, expected);
            }
            try (final RocksDB rocksDB = RocksDB.open(options, "./rocksdb-data/")) {
                assertArrayEquals(expected, rocksDB.get(key));
            }
        }
    }
    
    @Test
    public void testRocksDBRepository() throws IOException {
        Configuration configuration = new Configuration();
        configuration.setPeer(new Peer("localhost", 21630));
        try (LogRepository logRepository = new RocksDBRepository(configuration)) {
            LogEntry logEntry = new LogEntry(new LogId(0, 1), configuration);
            for (int i = 0; i < 3; i++) {
                logRepository.appendEntry(logEntry);
            }
        }
    }
}
