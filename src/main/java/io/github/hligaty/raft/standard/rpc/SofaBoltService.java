package io.github.hligaty.raft.standard.rpc;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import io.github.hligaty.raft.standard.Node;
import io.github.hligaty.raft.standard.rpc.packet.LogEntryReq;
import io.github.hligaty.raft.standard.rpc.packet.VoteReq;

public class SofaBoltService implements RpcService {

    private final Node node;

    public SofaBoltService(int port, Node node) {
        RpcServer rpcServer = new RpcServer(port);
        rpcServer.registerUserProcessor(new SyncUserProcessor<Request>() {
            @Override
            public Object handleRequest(BizContext bizCtx, Request request) {
                return SofaBoltService.this.handleRequest(request);
            }

            @Override
            public String interest() {
                return Request.class.getName();
            }
        });
        rpcServer.startup();
        this.node = node;
    }

    @Override
    public Response handleRequest(Request request) {
        return switch (request.data()) {
            case VoteReq voteReq:
                yield new Response(node.voteFor(voteReq));
            case LogEntryReq logEntryReq:
                yield new Response(node.appendEntry(logEntryReq));
            default:
                throw new IllegalStateException("Unexpected value: " + request.data());
        };
    }
}
