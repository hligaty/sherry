package io.github.hligaty.raft.rpc.packet;

import io.github.hligaty.raft.util.Endpoint;

import java.io.Serializable;

public record RequestVoteRequest(
        Endpoint endpoint,
        long term,
        long lastLogIndex,
        long lastLogTerm
) implements Serializable {
}
