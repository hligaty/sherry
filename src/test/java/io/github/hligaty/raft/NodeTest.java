package io.github.hligaty.raft;

import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import io.github.hligaty.BaseTest;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.core.DefaultNode;
import io.github.hligaty.raft.rpc.packet.Command;
import io.github.hligaty.raft.util.Peer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeTest extends BaseTest {

    List<Peer> allPeers = Stream.of(4869, 4870, 4871)
            .map(port -> new Peer("localhost", port))
            .toList();

    @Test
    public void node() {
        int port = Integer.parseInt(System.getProperty("port"));
        List<Peer> peers = new ArrayList<>(allPeers);
        peers.removeIf(peer -> peer.port() == port);
        Configuration configuration = new Configuration()
                .setPeer(new Peer("localhost", port))
                .addPeers(peers);
        Node node = new DefaultNode(new KVStateMachine(String.valueOf(port)));
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }

    @Test
    public void set() throws RemotingException, InterruptedException {
        String address = "localhost";
        int port = 4870;
        RpcClient rpcClient = new RpcClient();
        rpcClient.startup();
        Url url = new Url(address, port);
        url.setProtocol(RpcProtocol.PROTOCOL_CODE);
        KVStateMachine.Put put = new KVStateMachine.Put();
        put.key = "foo";
        put.value = "bar";
//        rpcClient.invokeSync(url, new Command(put), 5000);
        KVStateMachine.Get get = new KVStateMachine.Get();
        get.key = put.key;
        String value = (String) rpcClient.invokeSync(url, new Command(get), 5000);
        assertEquals(put.value, value);
    }
}
