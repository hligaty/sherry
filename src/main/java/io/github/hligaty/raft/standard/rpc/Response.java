package io.github.hligaty.raft.standard.rpc;

import java.io.Serializable;

public record Response(Object data) implements Serializable {

}
