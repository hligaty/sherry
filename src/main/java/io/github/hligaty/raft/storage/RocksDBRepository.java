package io.github.hligaty.raft.storage;

import io.fury.Fury;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.rpc.packet.Command;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class RocksDBRepository implements LogRepository {

    private static final LogEntry FIRST_LOG_ENTRY = new LogEntry(new LogId(0, 0), new Command(null));

    private static final Fury serializer;

    static {
        serializer = Fury.builder()
                .requireClassRegistration(false)
                .build();
        RocksDB.loadLibrary();
    }

    private final RocksDB db;

    private final Options options;

    private final ReadOptions totalOrderReadOptions;


    public RocksDBRepository(Configuration configuration) {
        this.options = new Options().setCreateIfMissing(true);
        try {
            this.db = RocksDB.open(this.options, "./rocksdb-log-" + configuration.getPeer().port() + "/");
            if (db.get(getKeyBytes(0)) == null) {
                db.put(getKeyBytes(0), serializer.serializeJavaObject(FIRST_LOG_ENTRY));
            }
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
        this.totalOrderReadOptions = new ReadOptions();
        this.totalOrderReadOptions.setTotalOrderSeek(true);
    }

    @Override
    public long appendEntry(LogEntry logEntry) {
        try {
            long lastLogIndex = getLastLogIndex();
            logEntry = logEntry.setLogIndex(lastLogIndex + 1);
            byte[] valueBytes = serializer.serializeJavaObject(logEntry);
            db.put(getKeyBytes(logEntry.logId().index()), valueBytes);
            return logEntry.logId().index();
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    private byte[] getKeyBytes(long index) {
        return serializer.serializeJavaObject(index);
    }

    public void appendEntries(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return;
        }
        try (WriteBatch batch = new WriteBatch()) {
            for (LogEntry logEntry : logEntries) {
                byte[] valueBytes = serializer.serializeJavaObject(logEntry);
                batch.put(getKeyBytes(logEntry.logId().index()), valueBytes);
            }
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public LogEntry getEntry(long index) {
        if (index == 0) {
            return FIRST_LOG_ENTRY;
        }
        try {
            byte[] valueBytes = db.get(getKeyBytes(index));
            return valueBytes == null ? null : serializer.deserializeJavaObject(valueBytes, LogEntry.class);
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public long getLastLogIndex() {
        try (final RocksIterator it = this.db.newIterator(this.totalOrderReadOptions)) {
            it.seekToLast();
            byte[] key = it.key();
            return serializer.deserializeJavaObject(key, long.class);
        }
    }

    @Override
    public LogId getLastLogId() {
        try (final RocksIterator it = this.db.newIterator(this.totalOrderReadOptions)) {
            it.seekToLast();
            byte[] value = it.value();
            return serializer.deserializeJavaObject(value, LogEntry.class).logId();
        }
    }

    @Override
    public List<LogEntry> getSuffix(long beginIndex) {
        return getSuffix(beginIndex, __ -> false);
    }

    public List<LogEntry> getSuffix(long beginIndex, Function<RocksIterator, Boolean> breakFunction) {
        try (final RocksIterator it = this.db.newIterator(this.totalOrderReadOptions)) {
            it.seek(getKeyBytes(beginIndex));
            if (!it.isValid() || serializer.deserializeJavaObject(it.key(), long.class) != beginIndex) {
                throw new StoreException("没有索引为%s开头的日志".formatted(beginIndex));
            }
            List<LogEntry> result = new ArrayList<>();
            while (it.isValid() && !breakFunction.apply(it)) {
                result.add(serializer.deserializeJavaObject(it.value(), LogEntry.class));
                it.next();
            }
            return result;
        }
    }

    @Override
    public List<LogEntry> getSuffix(long beginIndex, long endIndex) {
        if (beginIndex >= endIndex) {
            return Collections.emptyList();
        }
        return getSuffix(beginIndex, it -> serializer.deserializeJavaObject(it.key(), long.class) > endIndex);
    }

    @Override
    public void truncateSuffix(long lastIndexKept) {
        try {
            db.deleteRange(getKeyBytes(lastIndexKept + 1), getKeyBytes(getLastLogIndex() + 1));
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public void close() {
        db.close();
        options.close();
        totalOrderReadOptions.close();
    }
}
