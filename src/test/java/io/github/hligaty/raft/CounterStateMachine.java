package io.github.hligaty.raft;

import io.fury.Fury;
import io.github.hligaty.raft.rpc.packet.Command;
import io.github.hligaty.raft.storage.StoreException;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class CounterStateMachine implements StateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(KVStateMachine.class);

    private static final Fury serializer;

    static {
        serializer = Fury.builder()
                .requireClassRegistration(false)
                .build();
        RocksDB.loadLibrary();
    }

    private final RocksDB db;

    public CounterStateMachine(Path dir) {
        Options options = new Options().setCreateIfMissing(true);
        try {
            Path path = dir.resolve("data-kv-rocksdb");
            Files.createDirectories(path);
            this.db = RocksDB.open(options, path.toString());
        } catch (RocksDBException | IOException e) {
            throw new StoreException(e);
        }
    }

    private static final byte[] COUNTER_IDX_KEY = serializer.serializeJavaObject("counter");

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Serializable> R apply(Command command) {
        try {
            switch (command.getData()) {
                case Increment ignored: {
                    
                    byte[] valueBytes = db.get(COUNTER_IDX_KEY);
                    Long count = valueBytes == null ? 0L : serializer.deserializeJavaObject(valueBytes, Long.class);
                    db.put(COUNTER_IDX_KEY, serializer.serializeJavaObject(++count));
                    return (R) count;
                }
                case Get ignored: {
                    return (R) serializer.deserializeJavaObject(db.get(COUNTER_IDX_KEY), Long.class);
                }
                case null, default:
                    throw new IllegalStateException("Unexpected value: " + command.getData());
            }
        } catch (Exception e) {
            LOG.error("设置到状态机错误", e);
            return null;
        }
    }

    public static class Increment implements Serializable {
    }

    public static class Get implements Serializable {
    }
}
