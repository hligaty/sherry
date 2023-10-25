package io.github.hligaty.raft.infrastructure;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import com.alipay.remoting.rpc.protocol.SyncMultiInterestUserProcessor;
import io.github.hligaty.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RpcTest extends BaseTest {

    @Test
    public void test() {
        String address = "localhost";
        int port = 4869;
        RpcServer rpcServer = new RpcServer(port);
        rpcServer.registerUserProcessor(new SyncMultiInterestUserProcessor<>() {
            @Override
            public Object handleRequest(BizContext bizCtx, Object request) {
                return request;
            }

            @Override
            public List<String> multiInterest() {
                return List.of(String.class.getName(), Integer.class.getName());
            }

            @Override
            public Executor getExecutor() {
                return Executors.newVirtualThreadPerTaskExecutor();
            }
        });
        rpcServer.startup();
        RpcClient rpcClient = new RpcClient();
        rpcClient.startup();
        try {
            Url url = new Url(address, port);
            url.setProtocol(RpcProtocol.PROTOCOL_CODE);
            Object expected = "hello sherry";
            Object actual = rpcClient.invokeSync(url, expected, 100);
            assertEquals(expected, actual);
            expected = 1;
            actual = rpcClient.invokeSync(url, expected, 2);
            assertEquals(expected, actual);
        } catch (RemotingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
