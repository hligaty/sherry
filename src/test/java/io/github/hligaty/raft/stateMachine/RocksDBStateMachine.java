package io.github.hligaty.raft.stateMachine;

import io.fury.Fury;
import io.github.hligaty.raft.StateMachine;
import io.github.hligaty.raft.storage.StoreException;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class RocksDBStateMachine implements StateMachine {

    protected static final Logger LOG = LoggerFactory.getLogger(KVStateMachine.class);

    protected static final Fury serializer;

    static {
        serializer = Fury.builder()
                .requireClassRegistration(false)
                .build();
        RocksDB.loadLibrary();
    }

    protected RocksDB db;
    
    public final void startup(Path dir) {
        Options options = new Options().setCreateIfMissing(true);
        try {
            Path path = dir.resolve("data-rocksdb");
            Files.createDirectories(path);
            this.db = RocksDB.open(options, path.toString());
        } catch (RocksDBException | IOException e) {
            throw new StoreException(e);
        }
    }
    
    
}
