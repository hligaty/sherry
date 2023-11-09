package io.github.hligaty.raft;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import io.github.hligaty.BaseTest;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.core.DefaultNode;
import io.github.hligaty.raft.rpc.packet.Command;
import io.github.hligaty.raft.util.PeerId;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class RaftLocalClusterCliTest extends BaseTest {

    private static final List<PeerId> allPeerIds = Stream.of(4869, 4870, 4871)
            .map(port -> new PeerId("localhost", port))
            .toList();
    
    private static Integer leaderPort;

    private static final RpcClient rpcClient = new RpcClient();
    
    public static void main(String[] args) {
        rpcClient.startup();
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String content = scanner.nextLine();
                String[] params = Arrays.stream(content.split(" "))
                        .filter(param -> !param.isBlank())
                        .toArray(String[]::new);
                if ("raft".equals(params[0]) && "set".equals(params[1])) {
                    // 设置领导者命令: raft set 4869
                    leaderPort = Integer.parseInt(params[2]);
                } else {
                    executeKVCommand(params);
                }
                System.out.println("Please enter the next command:");
            }
        }
    }

    /**
     * 启动参数(复制 VM 参数的 IDEA 小技巧: 鼠标左键放在 -ea 前面的同时按住键盘 Alt, 向右拖动鼠标, 即可复制没有 Javadoc 星号的 VM 参数):
     * -ea
     * -Dport=4869
     * --add-opens=java.base/java.lang=ALL-UNNAMED
     * --add-opens=java.base/java.io=ALL-UNNAMED
     * --add-opens=java.base/java.math=ALL-UNNAMED
     * --add-opens=java.base/java.net=ALL-UNNAMED
     * --add-opens=java.base/java.nio=ALL-UNNAMED
     * --add-opens=java.base/java.security=ALL-UNNAMED
     * --add-opens=java.base/java.text=ALL-UNNAMED
     * --add-opens=java.base/java.time=ALL-UNNAMED
     * --add-opens=java.base/java.util=ALL-UNNAMED
     * --add-opens=java.base/jdk.internal.access=ALL-UNNAMED
     * --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
     */
    @Test
    public void startupNode() {
        int port = Integer.parseInt(System.getProperty("port"));
        List<PeerId> peerIds = new ArrayList<>(allPeerIds);
        peerIds.removeIf(peer -> peer.port() == port);
        Configuration configuration = new Configuration()
                .setPeer(new PeerId("localhost", port))
                .addPeers(peerIds);
        Node node = new DefaultNode(new KVStateMachine(configuration.getDataPath()));
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }

    private static void executeKVCommand(String[] args) {
        switch (args[0]) {
            case "put": {
                KVStateMachine.Put put = new KVStateMachine.Put();
                put.key = args[1];
                put.value = args[2];
                sendCommand(leaderPort, put);
                break;
            }
            case "get": {
                KVStateMachine.Get get = new KVStateMachine.Get();
                get.key = args[1];
                Object value = sendCommand(leaderPort, get);
                System.out.println(value);
                break;
            }
            case "delete": {
                KVStateMachine.Delete delete = new KVStateMachine.Delete();
                delete.key = args[1];
                sendCommand(leaderPort, delete);
            }
        }
    }

    static Object sendCommand(Integer leaderPort, Serializable data) {
        try {
            return rpcClient.invokeSync("localhost:" + leaderPort, new Command(data), 5000);
        } catch (RemotingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
