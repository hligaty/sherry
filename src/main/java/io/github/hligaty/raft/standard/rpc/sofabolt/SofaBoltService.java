package io.github.hligaty.raft.standard.rpc.sofabolt;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import io.github.hligaty.raft.standard.rpc.Request;
import io.github.hligaty.raft.standard.rpc.Response;
import io.github.hligaty.raft.standard.rpc.RpcService;

public class SofaBoltService implements RpcService {

    public SofaBoltService(int port) {
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
    }

    @Override
    public Response handleRequest(Request request) {
        return null;
    }
}
