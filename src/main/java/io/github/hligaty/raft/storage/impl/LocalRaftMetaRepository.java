package io.github.hligaty.raft.storage.impl;

import io.github.hligaty.raft.storage.RaftMetaRepository;
import io.github.hligaty.raft.storage.StoreException;
import io.github.hligaty.raft.rpc.packet.PeerId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LocalRaftMetaRepository implements RaftMetaRepository {

    private final Path path;

    private long term;

    private PeerId peerId = PeerId.emptyId();

    private long committedIndex;

    public LocalRaftMetaRepository(Path dir) {
        /*
        文件格式: 第一行是任期, 第二行是任期投票的节点, 第三行是最后提交的日志索引
         */
        this.path = dir.resolve("raft-meta");
        if (Files.notExists(path)) {
            save(term, peerId, committedIndex);
        } else {
            try {
                List<String> lines = Files.readAllLines(path);
                term = Long.parseLong(lines.get(0));
                peerId = PeerId.parse(lines.get(1));
                committedIndex = Long.parseLong(lines.get(2));
            } catch (IOException e) {
                throw new StoreException(e);
            }
        }
    }

    @Override
    public long getTerm() {
        return term;
    }

    @Override
    public PeerId getVotedFor() {
        return peerId;
    }

    @Override
    public long getCommittedIndex() {
        return committedIndex;
    }

    @Override
    public void setTermAndVotedFor(long term, PeerId peerId) {
        save(term, peerId, committedIndex);
    }

    private void save(long term, PeerId peerId, long committedIndex) {
        if (
                term == this.term
                && peerId.equals(this.peerId)
                && committedIndex == this.committedIndex
        ) {
            return;
        }
        try {
            Files.write(path, List.of(
                    String.valueOf(term),
                    peerId.toString(),
                    String.valueOf(committedIndex)
            ));
            this.term = term;
            this.peerId = peerId;
            this.committedIndex = committedIndex;
        } catch (IOException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public void saveCommittedIndex(long committedIndex) {
        save(term, peerId, committedIndex);
    }
}
