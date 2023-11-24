package io.github.hligaty.raft.util;

import io.fury.Fury;
import org.rocksdb.RocksDB;

public class Serializer {

    private static final Serializer INSTANCE = new Serializer();

    private static final Fury fury;

    static {
        fury = Fury.builder()
                .requireClassRegistration(false)
                .build();
        RocksDB.loadLibrary();
    }

    public static Serializer getInstance() {
        return INSTANCE;
    }

    public byte[] serialize(Object obj) {
        return fury.serializeJavaObject(obj);
    }

    public <T> T deserialize(byte[] data, Class<T> cls) {
        return fury.deserializeJavaObject(data, cls);
    }

}
