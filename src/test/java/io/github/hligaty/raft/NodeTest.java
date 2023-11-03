package io.github.hligaty.raft;

import io.github.hligaty.BaseTest;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.core.DefaultNode;
import io.github.hligaty.raft.util.Peer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
        Node node = new DefaultNode();
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }
}
