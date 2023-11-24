package io.github.hligaty.raft.stateMachine;

import org.rocksdb.RocksDBException;

import java.io.Serializable;

public class KVStateMachine extends RocksDBStateMachine {

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Serializable> R apply(Serializable data) throws RocksDBException {
        switch (data) {
            case Get get -> {
                byte[] bytes = db.get(serializer.serialize(get.key));
                return bytes == null ? null : (R) serializer.deserialize(bytes, String.class);
            }
            case Set set -> {
                db.put(serializer.serialize(set.key), serializer.serialize(set.value));
                return null;
            }
            case Delete delete -> {
                db.delete(serializer.serialize(delete.key));
                return null;
            }
            case null, default -> {
                return null;
            }
        }
    }

    public static class Set implements Serializable {
        public String key;
        public String value;

        @Override
        public String toString() {
            return "Set{" +
                   "key='" + key + '\'' +
                   ", value='" + value + '\'' +
                   '}';
        }
    }

    public static class Get implements Serializable {
        public String key;

        @Override
        public String toString() {
            return "Get{" +
                   "key='" + key + '\'' +
                   '}';
        }
    }

    public static class Delete implements Serializable {
        public String key;

        @Override
        public String toString() {
            return "Delete{" +
                   "key='" + key + '\'' +
                   '}';
        }
    }
}
