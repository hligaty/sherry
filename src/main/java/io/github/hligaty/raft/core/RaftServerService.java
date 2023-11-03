package io.github.hligaty.raft.core;

import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.AppendEntriesResponse;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;

public interface RaftServerService {

    RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest requestVoteRequest);

    AppendEntriesResponse handleAppendEntriesRequest(AppendEntriesRequest appendEntriesRequest);
}
