package io.github.hligaty.raft;

import io.fury.Fury;
import io.github.hligaty.raft.rpc.packet.Command;
import io.github.hligaty.raft.storage.StoreException;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

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
    public <R extends Serializable> R apply(Command command) {
        try {
            switch (command.getData()) {
                case Get getRequest -> {
                    byte[] bytes = db.get(serializer.serializeJavaObject(getRequest.key));
                    return bytes == null ? null : (R) serializer.deserialize(bytes);
                }
                case Put putRequest -> {
                    db.put(serializer.serializeJavaObject(putRequest.key), serializer.serialize(putRequest.value));
                    return null;
                }
                case Delete deleteRequest -> {
                    db.delete(serializer.serializeJavaObject(deleteRequest.key));
                    return null;
                }
                case null, default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            LOG.error("设置到状态机错误", e);
            return null;
        }
    }
    
    public static class Put implements Serializable {
        public String key;
        public String value;
    }
    
    public static class Get implements Serializable {
        public String key;
    }

    public static class Delete implements Serializable {
        public String key;
    }
}
