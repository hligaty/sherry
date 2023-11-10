package io.github.hligaty.raft.storage;

import io.github.hligaty.raft.rpc.packet.PeerId;

public interface RaftMetaRepository {
    
    long getTerm();
    
    PeerId getVotedFor();

    long getCommittedIndex();

    void setTermAndVotedFor(long term, PeerId peerId);

    void saveCommittedIndex(long committedIndex);
    
}
