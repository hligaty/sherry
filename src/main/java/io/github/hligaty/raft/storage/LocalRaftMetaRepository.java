package io.github.hligaty.raft.storage;

import io.github.hligaty.raft.util.PeerId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LocalRaftMetaRepository implements RaftMetaRepository {

    private final Path path;

    public LocalRaftMetaRepository(Path dir) {
        /*
        文件格式: 第一行是任期, 第二行投票节点的地址, 第三行是投票节点的端口
         */
        this.path = dir.resolve("raft-meta");
        if (Files.notExists(path)) {
            setTermAndVotedFor(0, PeerId.emptyId());
        }
    }

    @Override
    public long getTerm() {
        try {
            List<String> lines = Files.readAllLines(path);
            return lines.isEmpty() ? 0 : Long.parseLong(lines.get(0));
        } catch (IOException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public PeerId getVotedFor() {
        try {
            List<String> lines = Files.readAllLines(path);
            return lines.isEmpty() ? null : new PeerId(lines.get(1), Integer.parseInt(lines.get(2)));
        } catch (IOException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public void setTermAndVotedFor(long term, PeerId peerId) {
        try {
            Files.write(path, List.of(
                    String.valueOf(term),
                    peerId.address(),
                    String.valueOf(peerId.port())
            ));
        } catch (IOException e) {
            throw new StoreException(e);
        }
    }
}
