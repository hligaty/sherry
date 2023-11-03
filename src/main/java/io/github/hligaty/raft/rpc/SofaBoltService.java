package io.github.hligaty.raft.rpc;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import com.google.common.base.CaseFormat;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.core.RaftServerService;
import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.util.Peer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SofaBoltService implements RpcService {

    private final Configuration configuration;

    private final RaftServerService raftServerService;

    private final RpcClient rpcClient;


    public SofaBoltService(Configuration configuration, RaftServerService raftServerService) {
        this.configuration = configuration;
        this.raftServerService = raftServerService;
        RpcServer rpcServer = new RpcServer(configuration.getPeer().port());
        rpcServer.registerUserProcessor(
                new SingleThreadExecutorSyncUserProcessor<>(RequestVoteRequest.class.getName())
        );
        rpcServer.registerUserProcessor(
                new SingleThreadExecutorSyncUserProcessor<>(AppendEntriesRequest.class.getName())
        );
        rpcServer.startup();
        this.rpcClient = new RpcClient();
        rpcClient.startup();
    }

    @Override
    public Object handleRequest(Object request) {
        return switch (request) {
            case RequestVoteRequest requestVoteRequest:
                yield raftServerService.handleRequestVoteRequest(requestVoteRequest);
            case AppendEntriesRequest appendEntriesRequest:
                yield raftServerService.handleAppendEntriesRequest(appendEntriesRequest);
            default:
                throw new IllegalStateException("Unexpected value: " + request);
        };
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) throws RpcException {
        Peer peer = rpcRequest.peer();
        Url url = new Url(peer.address(), peer.port());
        url.setProtocol(RpcProtocol.PROTOCOL_CODE);
        url.setConnectTimeout(configuration.getRpcConnectTimeoutMs());
        try {
            return rpcClient.invokeSync(url, rpcRequest.request(), rpcRequest.timeoutMillis());
        } catch (RemotingException | InterruptedException e) {
            throw new RpcException(e);
        }
    }

    private class SingleThreadExecutorSyncUserProcessor<T> extends SyncUserProcessor<T> {

        final String interest;

        final Executor executor;

        public SingleThreadExecutorSyncUserProcessor(String interest) {
            this.interest = interest;
            String simpleInterest = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, interest.substring(interest.lastIndexOf('.') + 1));
            String threadNamePrefix = simpleInterest + "-process-thread";
            this.executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name(threadNamePrefix).factory());
        }

        @Override
        public Object handleRequest(BizContext bizCtx, Object request) {
            return SofaBoltService.this.handleRequest(request);
        }

        @Override
        public String interest() {
            return interest;
        }

        @Override
        public Executor getExecutor() {
            return executor;
        }
    }
}
