package io.github.hligaty.raft;

import io.github.hligaty.raft.rpc.packet.Command;

import java.io.Serializable;

public interface StateMachine {

    <R extends Serializable> R apply(Command command);

}
