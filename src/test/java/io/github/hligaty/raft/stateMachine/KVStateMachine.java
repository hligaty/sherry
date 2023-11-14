package io.github.hligaty.raft.stateMachine;

import io.github.hligaty.raft.rpc.packet.Command;

import java.io.Serializable;

public class KVStateMachine extends RocksDBStateMachine {

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Serializable> R apply(Command command) {
        try {
            switch (command.data()) {
                case Get getRequest -> {
                    byte[] bytes = db.get(serializer.serializeJavaObject(getRequest.key));
                    return bytes == null ? null : (R) serializer.deserialize(bytes);
                }
                case Set setRequest -> {
                    db.put(serializer.serializeJavaObject(setRequest.key), serializer.serialize(setRequest.value));
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
