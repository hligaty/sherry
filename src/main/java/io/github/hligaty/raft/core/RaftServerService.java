package io.github.hligaty.raft.core;

import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.AppendEntriesResponse;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;

import java.io.Serializable;

public interface RaftServerService {

    RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest requestVoteRequest);

    AppendEntriesResponse handleAppendEntriesRequest(AppendEntriesRequest appendEntriesRequest);

    <T extends Serializable, R extends Serializable> R handleClientRequest(T data) throws ApplyException;
}
