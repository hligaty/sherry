package io.github.hligaty.raft.core;

import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.AppendEntriesResponse;
import io.github.hligaty.raft.rpc.packet.ClientRequest;
import io.github.hligaty.raft.rpc.packet.ClientResponse;
import io.github.hligaty.raft.rpc.packet.ReadIndexRequest;
import io.github.hligaty.raft.rpc.packet.ReadIndexResponse;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteResponse;

public interface RaftServerService {

    RequestVoteResponse handleRequestVoteRequest(RequestVoteRequest requestVoteRequest);

    AppendEntriesResponse handleAppendEntriesRequest(AppendEntriesRequest appendEntriesRequest);

    ClientResponse handleClientRequest(ClientRequest clientRequest) throws ServerException;
    
    ReadIndexResponse handleReadIndexRequest(ReadIndexRequest readIndexRequest);
}
