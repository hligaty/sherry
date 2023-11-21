package io.github.hligaty.raft.rpc;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import com.google.common.base.CaseFormat;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.core.RaftServerService;
import io.github.hligaty.raft.rpc.packet.AppendEntriesRequest;
import io.github.hligaty.raft.rpc.packet.ClientRequest;
import io.github.hligaty.raft.rpc.packet.PeerId;
import io.github.hligaty.raft.rpc.packet.ReadIndexRequest;
import io.github.hligaty.raft.rpc.packet.RequestVoteRequest;
import io.github.hligaty.raft.rpc.packet.Traceable;
import io.github.hligaty.raft.util.Tracker;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SofaBoltService implements RpcService {

    private final Configuration configuration;

    private final RaftServerService raftServerService;

    private final RpcServer rpcServer;

    private final RpcClient rpcClient;


    public SofaBoltService(Configuration configuration, RaftServerService raftServerService) {
        this.configuration = configuration;
        this.raftServerService = raftServerService;
        this.rpcServer = new RpcServer(configuration.getServerId().port());
        rpcServer.registerUserProcessor(
                new SuperAsyncUserProcessor<>(RequestVoteRequest.class.getName())
        );
        rpcServer.registerUserProcessor(
                new SingleThreadExecutorSyncUserProcessor<>(AppendEntriesRequest.class.getName())
        );
        rpcServer.registerUserProcessor(
                new SingleThreadExecutorSyncUserProcessor<>(ReadIndexRequest.class.getName())
        );
        rpcServer.registerUserProcessor(
                new SingleThreadExecutorSyncUserProcessor<>(ClientRequest.class.getName())
        );
        rpcServer.startup();
        this.rpcClient = new RpcClient();
        rpcClient.startup();
    }

    @Override
    public Object handleRequest(Object request) {
        if (request instanceof Traceable traceable) {
            Tracker.start(traceable.traceId());
        }
        try {
            return switch (request) {
                case AppendEntriesRequest appendEntriesRequest:
                    yield raftServerService.handleAppendEntriesRequest(appendEntriesRequest);
                case ReadIndexRequest readIndexRequest:
                    yield raftServerService.handleReadIndexRequest(readIndexRequest);
                case RequestVoteRequest requestVoteRequest:
                    yield raftServerService.handleRequestVoteRequest(requestVoteRequest);
                case ClientRequest clientRequest:
                    yield raftServerService.handleClientRequest(clientRequest);
                default:
                    throw new IllegalStateException("Unexpected value: " + request.getClass());
            };
        } finally {
            Tracker.stop();
        }
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) throws RpcException {
        PeerId peerId = rpcRequest.remoteId();
        Url url = new Url(peerId.address(), peerId.port());
        url.setProtocol(RpcProtocol.PROTOCOL_CODE);
        url.setConnectTimeout(configuration.getRpcConnectTimeoutMs());
        try {
            return rpcClient.invokeSync(url, rpcRequest.request(), rpcRequest.timeoutMillis());
        } catch (RemotingException | InterruptedException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public void shutdown() {
        rpcClient.shutdown();
        rpcServer.shutdown();
    }

    private static String generateThreadNameFromInterest(String interest) {
        // "fooBar" -> "foo-bar-process-thread"
        String simpleInterest = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, interest.substring(interest.lastIndexOf('.') + 1));
        return simpleInterest + "-process-thread";
    }

    private class SingleThreadExecutorSyncUserProcessor<T> extends SyncUserProcessor<T> {

        final String interest;

        final Executor executor;

        public SingleThreadExecutorSyncUserProcessor(String interest) {
            this.interest = interest;
            String threadNamePrefix = generateThreadNameFromInterest(interest);
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

    private class SuperAsyncUserProcessor<T> extends AsyncUserProcessor<T> {

        final String interest;

        final Executor executor;

        public SuperAsyncUserProcessor(String interest) {
            this.interest = interest;
            String threadNamePrefix = generateThreadNameFromInterest(interest);
            this.executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix).factory());
        }

        @Override
        public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) {
            Object responseObject = SofaBoltService.this.handleRequest(request);
            asyncCtx.sendResponse(responseObject);
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
