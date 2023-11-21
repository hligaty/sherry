package io.github.hligaty.raft;

import java.io.Serializable;

public interface StateMachine {

    <R extends Serializable> R apply(Serializable data) throws Exception;

}
