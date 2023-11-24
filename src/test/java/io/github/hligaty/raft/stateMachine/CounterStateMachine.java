package io.github.hligaty.raft.stateMachine;

import org.rocksdb.RocksDBException;

import java.io.Serializable;

public class CounterStateMachine extends RocksDBStateMachine {

    private static final byte[] COUNTER_IDX_KEY = serializer.serialize("counter");

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Serializable> R apply(Serializable data) throws RocksDBException {
        switch (data) {
            case Increment ignored: {
                byte[] valueBytes = db.get(COUNTER_IDX_KEY);
                Long count = valueBytes == null ? 0L : serializer.deserialize(valueBytes, Long.class);
                db.put(COUNTER_IDX_KEY, serializer.serialize(++count));
                return (R) count;
            }
            case Get ignored: {
                return (R) serializer.deserialize(db.get(COUNTER_IDX_KEY), Long.class);
            }
            case null, default:
                return null;
        }
    }

    public record Increment() implements Serializable {
    }

    public record Get() implements Serializable {
    }
}
