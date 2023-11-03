package io.github.hligaty.raft;

public interface StateMachine {

    <T, R> R apply(T data);

}
