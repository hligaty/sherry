package io.github.hligaty.raft.core;

import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.AppendEntriesResponse;
import io.github.hligaty.raft.rpc.packet.ReadIndexRequest;
import io.github.hligaty.raft.rpc.packet.ReadIndexResponse;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;
import io.github.hligaty.raft.rpc.packet.Command;

import java.io.Serializable;

public interface RaftServerService {

    RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest requestVoteRequest);

    AppendEntriesResponse handleAppendEntriesRequest(AppendEntriesRequest appendEntriesRequest);

    <R extends Serializable> R apply(Command command) throws ApplyException;
    
    ReadIndexResponse handleReadIndexRequest(ReadIndexRequest readIndexRequest);
}
