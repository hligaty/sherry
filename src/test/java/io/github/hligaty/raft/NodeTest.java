package io.github.hligaty.raft;

import io.github.hligaty.BaseTest;
import io.github.hligaty.raft.config.Configuration;
import io.github.hligaty.raft.core.DefaultNode;
import io.github.hligaty.raft.util.Endpoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class NodeTest extends BaseTest {

    List<Endpoint> allEndpoints = Stream.of(4869, 4870, 4871)
            .map(port -> new Endpoint("localhost", port))
            .toList();
    
    @Test
    public void node1() {
        List<Endpoint> endpoints = new ArrayList<>(allEndpoints);
        Endpoint endpoint = endpoints.remove(0);
        Configuration configuration = new Configuration()
                .setEndpoint(endpoint)
                .addPeerNodes(endpoints);
        Node node = new DefaultNode();
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }

    @Test
    public void node2() {
        List<Endpoint> endpoints = new ArrayList<>(allEndpoints);
        Endpoint endpoint = endpoints.remove(1);
        Configuration configuration = new Configuration()
                .setEndpoint(endpoint)
                .addPeerNodes(endpoints);
        Node node = new DefaultNode();
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }

    @Test
    public void node3() {
        List<Endpoint> endpoints = new ArrayList<>(allEndpoints);
        Endpoint endpoint = endpoints.remove(2);
        Configuration configuration = new Configuration()
                .setEndpoint(endpoint)
                .addPeerNodes(endpoints);
        Node node = new DefaultNode();
        node.setConfiguration(configuration);
        node.startup();
        sleep();
    }
}
