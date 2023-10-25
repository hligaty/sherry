package io.github.hligaty.raft.standard.rpc;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import com.alipay.remoting.rpc.protocol.SyncMultiInterestUserProcessor;
import io.github.hligaty.raft.standard.Node;
import io.github.hligaty.raft.standard.util.Endpoint;
import io.github.hligaty.raft.standard.rpc.packet.AppendEntryRequest;
import io.github.hligaty.raft.standard.rpc.packet.RequestVoteRequest;

import java.util.List;

public class SofaBoltService implements RpcService {

    private final Node node;
    private final RpcClient rpcClient;
    

    public SofaBoltService(int port, Node node) {
        this.node = node;
        RpcServer rpcServer = new RpcServer(port);
        rpcServer.registerUserProcessor(new SyncMultiInterestUserProcessor<>() {
            @Override
            public Object handleRequest(BizContext bizCtx, Object request) {
                return SofaBoltService.this.handleRequest(request);
            }

            @Override
            public List<String> multiInterest() {
                return List.of(
                        RequestVoteRequest.class.getName(),
                        AppendEntryRequest.class.getName()
                );
            }
        });
        rpcServer.startup();
        this.rpcClient = new RpcClient();
    }

    @Override
    public Object handleRequest(Object request) {
        return switch (request) {
            case RequestVoteRequest requestVoteRequest:
                yield node.handleVoteRequest(requestVoteRequest);
            case AppendEntryRequest appendEntryRequest:
                yield node.appendEntry(appendEntryRequest);
            default:
                throw new IllegalStateException("Unexpected value: " + request);
        };
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) throws RpcException {
        Endpoint endpoint = rpcRequest.endpoint();
        Url url = new Url(endpoint.address(), endpoint.port());
        url.setProtocol(RpcProtocol.PROTOCOL_CODE);
        try {
            return rpcClient.invokeSync(url, rpcRequest.request(), rpcRequest.timeoutMillis());
        } catch (RemotingException | InterruptedException e) {
            throw new RpcException(e);
        }
    }
}
