package io.github.hligaty.raft.storage.impl;

import io.github.hligaty.raft.rpc.packet.ClientRequest;
import io.github.hligaty.raft.rpc.packet.LogEntry;
import io.github.hligaty.raft.rpc.packet.LogId;
import io.github.hligaty.raft.storage.LogRepository;
import io.github.hligaty.raft.storage.StoreException;
import io.github.hligaty.raft.util.Serializer;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class RocksDBLogRepository implements LogRepository {

    private final Serializer serializer = Serializer.getInstance();

    private final Lock lock = new ReentrantLock();

    private final RocksDB db;

    private final DBOptions dbOptions;

    private final WriteOptions writeOptions;

    private final ReadOptions totalOrderReadOptions;

    private final ColumnFamilyHandle defaultHandle;

    private LogId lastLogId;

    public RocksDBLogRepository(Path dir) {
        this.dbOptions = new DBOptions().setCreateIfMissing(true);
        this.writeOptions = new WriteOptions();
        this.writeOptions.setSync(true); // 同步刷盘非常耗费性能, 生产的实现一定要用批量刷盘
        this.totalOrderReadOptions = new ReadOptions();
        this.totalOrderReadOptions.setTotalOrderSeek(true);
        try {
            Path logDir = dir.resolve("log-rocksdb");
            if (Files.notExists(logDir)) {
                Files.createDirectory(logDir);
            }
            List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
            List<ColumnFamilyDescriptor> columnFamilyDescriptors = Collections.singletonList(
                    new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY)
            );
            this.db = RocksDB.open(dbOptions, logDir.toString(), columnFamilyDescriptors, columnFamilyHandles);
            assert columnFamilyHandles.size() == 1;
            this.defaultHandle = columnFamilyHandles.get(0);
            if (getEntry(0) == null) {
                db.put(defaultHandle, writeOptions, getKeyBytes(0), serializer.serialize(new LogEntry(LogId.zero(), ClientRequest.noop().data())));
            }
            lastLogId = getLastLogId();
        } catch (RocksDBException | IOException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public LogEntry appendEntry(long term, Serializable data) {
        lock.lock();
        try {
            long lastLogIndex = getLastLogIndex();
            LogEntry logEntry = new LogEntry(new LogId(term, lastLogIndex + 1), data);
            byte[] valueBytes = serializer.serialize(logEntry);
            db.put(defaultHandle, writeOptions, getKeyBytes(logEntry.index()), valueBytes);
            lastLogId = logEntry.logId();
            return logEntry;
        } catch (RocksDBException e) {
            throw new StoreException(e);
        } finally {
            lock.unlock();
        }
    }

    private byte[] getKeyBytes(long index) {
        return ByteBuffer.allocate(8).putLong(index).array();
    }

    private long getKey(byte[] keyBytes) {
        return ByteBuffer.wrap(keyBytes).getLong();
    }

    public void appendEntries(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return;
        }
        lock.lock();
        try (WriteBatch batch = new WriteBatch()) {
            for (LogEntry logEntry : logEntries) {
                byte[] valueBytes = serializer.serialize(logEntry);
                batch.put(defaultHandle, getKeyBytes(logEntry.index()), valueBytes);
            }
            db.write(writeOptions, batch);
            lastLogId = logEntries.getLast().logId();
        } catch (RocksDBException e) {
            throw new StoreException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogEntry getEntry(long index) {
        lock.lock();
        try {
            byte[] valueBytes = db.get(defaultHandle, totalOrderReadOptions, getKeyBytes(index));
            return valueBytes == null ? null : serializer.deserialize(valueBytes, LogEntry.class);
        } catch (RocksDBException e) {
            throw new StoreException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getLastLogIndex() {
        lock.lock();
        try {
            return lastLogId.index();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogId getLastLogId() {
        lock.lock();
        try {
            if (lastLogId != null) {
                return lastLogId;
            }
            try (final RocksIterator it = db.newIterator(defaultHandle, totalOrderReadOptions)) {
                it.seekToLast();
                byte[] value = it.value();
                return serializer.deserialize(value, LogEntry.class).logId();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<LogEntry> getSuffix(long beginIndex) {
        return getSuffix(beginIndex, __ -> false);
    }

    public List<LogEntry> getSuffix(long beginIndex, Function<RocksIterator, Boolean> breakFunction) {
        lock.lock();
        try {
            if (beginIndex > lastLogId.index()) {
                return Collections.emptyList();
            }
            try (final RocksIterator it = db.newIterator(defaultHandle, totalOrderReadOptions)) {
                it.seek(getKeyBytes(beginIndex));
                if (!it.isValid()) {
                    return Collections.emptyList();
                }
                List<LogEntry> result = new ArrayList<>();
                while (it.isValid() && !breakFunction.apply(it)) {
                    result.add(serializer.deserialize(it.value(), LogEntry.class));
                    it.next();
                }
                return result;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<LogEntry> getSuffix(long beginIndex, long endIndex) {
        if (beginIndex > endIndex) {
            return Collections.emptyList();
        }
        return getSuffix(beginIndex, it -> getKey(it.key()) > endIndex);
    }

    @Override
    public void truncateSuffix(long lastIndexKept) {
        lock.lock();
        try {
            if (lastIndexKept >= lastLogId.index()) {
                return;
            }
            db.deleteRange(defaultHandle, writeOptions, getKeyBytes(lastIndexKept + 1), getKeyBytes(getLastLogIndex() + 1));
        } catch (RocksDBException e) {
            throw new StoreException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        db.close();
        dbOptions.close();
        writeOptions.close();
        totalOrderReadOptions.close();
        defaultHandle.close();
    }
}
