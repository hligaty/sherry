package io.github.hligaty.raft;

import io.fury.Fury;
import io.github.hligaty.raft.storage.StoreException;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KVStateMachine implements StateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(KVStateMachine.class);

    private static final Fury serializer;

    static {
        serializer = Fury.builder()
                .requireClassRegistration(false)
                .build();
        RocksDB.loadLibrary();
    }

    private final RocksDB db;

    public KVStateMachine(String dir) {
        Options options = new Options().setCreateIfMissing(true);
        try {
            this.db = RocksDB.open(options, "./rocksdb-data-" + dir + "/");
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T, R> R apply(T data) {
        try {
            switch (data) {
                case Get get -> {
                    return (R) serializer.deserialize(db.get(serializer.serializeJavaObject(get.key)));
                }
                case Put put -> {
                    db.put(serializer.serializeJavaObject(put.key), serializer.serialize(put.value));
                    return null;
                }
                case Delete delete -> {
                    db.delete(serializer.serializeJavaObject(delete.key));
                    return null;
                }
                case null, default -> {
                    LOG.error("设置到状态机错误");
                    return null;
                }
            }
        } catch (RocksDBException e) {
            LOG.error("设置到状态机错误", e);
            return null;
        }
    }
    
    public static class Put {
        public String key;
        public String value;
    }
    
    public static class Get {
        public String key;
    }

    public static class Delete {
        public String key;
    }
}
