package io.github.hligaty.raft.standard;

import io.github.hligaty.BaseTest;
import io.github.hligaty.raft.standard.config.Configuration;
import io.github.hligaty.raft.standard.config.Endpoint;
import io.github.hligaty.raft.standard.core.DefaultNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class NodeTest extends BaseTest {

    @Test
    public void test() {
        int numsOfServer = 3;
        List<Endpoint> allEndpoints = new Random().ints(numsOfServer, 10000, 11000)
                .mapToObj(port -> new Endpoint("selfAddress", port))
                .toList();
        for (int i = 0; i < allEndpoints.size(); i++) {
            List<Endpoint> endpoints = new ArrayList<>(allEndpoints);
            Endpoint endpoint = endpoints.remove(i);
            Configuration configuration = new Configuration()
                    .setEndpoint(endpoint)
                    .addPeerNodes(endpoints);
            Node node = new DefaultNode();
            node.setConfiguration(configuration);
            node.start();
        }
        sleep();
    }
}
