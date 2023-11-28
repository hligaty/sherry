package io.github.hligaty.raft;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import io.github.hligaty.BaseTest;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.core.DefaultNode;
import io.github.hligaty.raft.core.ErrorType;
import io.github.hligaty.raft.rpc.packet.ClientRequest;
import io.github.hligaty.raft.rpc.packet.ClientResponse;
import io.github.hligaty.raft.rpc.packet.PeerId;
import io.github.hligaty.raft.stateMachine.CounterStateMachine;
import io.github.hligaty.raft.stateMachine.KVStateMachine;
import io.github.hligaty.raft.stateMachine.RocksDBStateMachine;
import io.github.hligaty.raft.util.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RaftLocalClusterTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(RaftLocalClusterTest.class);

    private static final RpcClient rpcClient = new RpcClient();

    private static Integer leaderPort;

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓  配置  ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    private static final List<PeerId> allPeerIds = Stream.of(4869, 4870, 4871, 4872, 4873)
            .map(port -> new PeerId("localhost", port))
            .toList();

//    private static final RocksDBStateMachine rocksDBStateMachine = new CounterStateMachine();
    private static final RocksDBStateMachine rocksDBStateMachine = new KVStateMachine();

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑  配置  ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑

    public static class RaftServer {
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
        public static void main(String[] args) {
            int port = Integer.parseInt(System.getProperty("port"));
            List<PeerId> peerIds = new ArrayList<>(allPeerIds);
            peerIds.removeIf(peer -> peer.port() == port);
            Configuration configuration = new Configuration()
                    .setServerId(new PeerId("localhost", port))
                    .addPeers(peerIds);
            rocksDBStateMachine.startup(configuration.getDataPath());
            Node node = new DefaultNode(rocksDBStateMachine);
            node.setConfiguration(configuration);
            node.startup();
            Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(node::shutdown));
        }
    }

    public static class RaftCli {
        /**
         * 启动参数(复制 VM 参数的 IDEA 小技巧: 鼠标左键放在 -ea 前面的同时按住键盘 Alt, 向右拖动鼠标, 即可复制没有 Javadoc 星号的 VM 参数):
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
        public static void main(String[] args) {
            rpcClient.startup();
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    long start = System.currentTimeMillis();
                    String content = scanner.nextLine();
                    String[] params = Arrays.stream(content.split(" "))
                            .filter(param -> !param.isBlank())
                            .toArray(String[]::new);
                    int times = 1;
                    String expected = null;
                    boolean enableResultValidate = false;
                    if (params.length > 2) {
                        for (int i = 0; i < params.length; i++) {
                            String param = params[i];
                            switch (param) {
                                case "-n": // "-n 5" 表示执行 5 次
                                    times = Integer.parseInt(params[i + 1]);
                                    break;
                                case "--assert": // "--assert foo" 表示预期返回结果是 foo
                                    enableResultValidate = true;
                                    expected = params[i + 1];
                            }
                        }
                    }
                    for (; times != 0; times--) {
                        Object actual = switch (rocksDBStateMachine) {
                            case KVStateMachine __:
                                yield executeKVCommand(params);
                            case CounterStateMachine __:
                                yield executeCounter(params);
                            default:
                                throw new IllegalStateException("如果你换成了自己写的状态机, 想继续用这个 CLI 测试的话需要在这里增加处理");
                        };
                        if (enableResultValidate) {
                            assertEquals(expected, actual != null ? actual.toString() : null);
                        }
                    }
                    System.out.printf("Please enter the command(command execution time%s):%n", System.currentTimeMillis() - start);
                }
            }
        }
    }

    private static Object executeKVCommand(String[] args) {
        switch (args[0]) {
            case "set": {
                KVStateMachine.Set set = new KVStateMachine.Set();
                set.key = args[1];
                set.value = args[1];
                return sendCommand(ClientRequest.write(set));
            }
            case "get": {
                KVStateMachine.Get get = new KVStateMachine.Get();
                get.key = args[1];
                Object value = sendCommand(ClientRequest.read(get));
                System.out.println(value);
                return value;
            }
            case "delete": {
                KVStateMachine.Delete delete = new KVStateMachine.Delete();
                delete.key = args[1];
                sendCommand(ClientRequest.write(delete));
                return null;
            }
            default:
                System.out.println("Unknown command.");
                return null;
        }
    }

    private static Object executeCounter(String[] args) {
        switch (args[0]) {
            case "increment": {
                CounterStateMachine.Increment increment = new CounterStateMachine.Increment();
                Object object = sendCommand(ClientRequest.write(increment));
                System.out.println(object);
                return object;
            }
            case "get": {
                CounterStateMachine.Get get = new CounterStateMachine.Get();
                Object value = sendCommand(ClientRequest.read(get));
                System.out.println(value);
                return value;
            }
            default:
                System.out.println("Unknown command.");
                return null;
        }
    }

    static Object sendCommand(ClientRequest request) {
        Tracer.start(request.traceId());
        try {
            List<PeerId> peerIds = new ArrayList<>(allPeerIds);
            if (leaderPort != null) { // 优先尝试刚才的集群领导者
                peerIds.removeIf(peerId -> peerId.port() == leaderPort);
                peerIds.add(0, new PeerId("localhost", leaderPort));
            }
            if (request.readOnly()) { // 如果是读命令就随机选一个处理
                Collections.shuffle(peerIds);
            }
            for (PeerId peerId : peerIds) {
                try {
                    long start = System.currentTimeMillis();
                    ClientResponse response = (ClientResponse) rpcClient.invokeSync("localhost:" + peerId.port(), request, 5000);
                    if (!request.readOnly()) {
                        leaderPort = peerId.port();
                    }
                    if (response.success()) {
                        LOG.info("Succeed to execute remote invoke. port[{}], leaderPort[{}], time[{}]",
                                peerId.port(), leaderPort, System.currentTimeMillis() - start);
                        return response.data();
                    }
                    if (response.errorType() == ErrorType.REPLICATION_FAIL) {
                        LOG.error("Failed to execute remote invoke, reason: [{}], port[{}], data[{}]", response.errorType(), peerId.port(), request.data());
                        return null;
                    }
                } catch (RemotingException | InterruptedException e) {
                    LOG.error("Failed to execute remote invoke, reason: [Unknown, {}].", e.getMessage());
                }
            }
            LOG.error("Failed to execute remote invoke, reason: all nodes timeout or not found cluster leader, data[{}]", request.data());
            return null;
        } finally {
            Tracer.stop();
        }
    }
}
