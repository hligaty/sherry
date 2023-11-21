package io.github.hligaty.raft.infrastructure;

import io.github.hligaty.raft.rpc.packet.ClientRequest;
import io.github.hligaty.raft.storage.impl.LocalRaftMetaRepository;
import io.github.hligaty.raft.storage.LogRepository;
import io.github.hligaty.raft.storage.impl.RocksDBLogRepository;
import io.github.hligaty.raft.storage.StoreException;
import io.github.hligaty.raft.rpc.packet.PeerId;
import org.junit.jupiter.api.Test;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class StoreTest {

    static {
        RocksDB.loadLibrary();
    }

    @Test
    public void test() throws RocksDBException {
        try (final Options options = new Options().setCreateIfMissing(true)) {
            byte[] key = "hello".getBytes();
            byte[] expected = "world".getBytes();
            try (final RocksDB rocksDB = RocksDB.open(options, "./rocksdb-data/")) {
                rocksDB.put(key, expected);
            }
            try (final RocksDB rocksDB = RocksDB.open(options, "./rocksdb-data/")) {
                assertArrayEquals(expected, rocksDB.get(key));
            }
        }
    }

    @Test
    public void testRocksDBRepository() throws IOException {
        Path path = Paths.get("testRocksDBRepository");
        Files.createDirectories(path);
        try (LogRepository logRepository = new RocksDBLogRepository(path)) {
            logRepository.appendEntry(0, ClientRequest.noop().data());
            long start = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                logRepository.appendEntry(0, ClientRequest.noop().data());
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    @Test
    public void testRaftMetaRepository() throws IOException {
        Path path = Paths.get("testRaftMetaRepository");
        Files.createDirectories(path);
        LocalRaftMetaRepository localRaftMetaRepository = new LocalRaftMetaRepository(path);
        localRaftMetaRepository.setTermAndVotedFor(0, new PeerId("127.0.0.1", 4869));
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            localRaftMetaRepository.setTermAndVotedFor(0, new PeerId("127.0.0.1", 4869));
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testMMAPRaftMetaRepository() throws IOException {
        Path path = Paths.get("testRaftMetaRepository");
        Files.createDirectories(path);
        Path file = path.resolve("file.txt");
        if (Files.notExists(file)) {
            Files.createFile(file);
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, 25);
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                mapped.position(0);
                mapped
                        .putLong(2).put((byte) '\n')
                        .put((byte) 127).put((byte) '.').put((byte) 0).put((byte) '.').put((byte) 0).put((byte) '.').put((byte) 1)
                        .putLong(4869);
                mapped.force();
            }
            System.out.println(System.currentTimeMillis() - start);
        } catch (IOException e) {
            throw new StoreException(e);
        }
    }
}
