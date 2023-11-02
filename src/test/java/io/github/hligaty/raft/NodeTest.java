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
    public void node1() {
        List<Peer> peers = new ArrayList<>(allPeers);
        Peer peer = peers.remove(0);
        Configuration configuration = new Configuration()
                .setPeer(peer)
                .addPeerNodes(peers);
        Node node = new DefaultNode();
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }

    @Test
    public void node2() {
        List<Peer> peers = new ArrayList<>(allPeers);
        Peer peer = peers.remove(1);
        Configuration configuration = new Configuration()
                .setPeer(peer)
                .addPeerNodes(peers);
        Node node = new DefaultNode();
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }

    @Test
    public void node3() {
        List<Peer> peers = new ArrayList<>(allPeers);
        Peer peer = peers.remove(2);
        Configuration configuration = new Configuration()
                .setPeer(peer)
                .addPeerNodes(peers);
        Node node = new DefaultNode();
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }
}
