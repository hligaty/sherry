package io.github.hligaty.raft.infrastructure;

import org.junit.jupiter.api.Test;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

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
}
