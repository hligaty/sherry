package io.github.hligaty.raft.core;

/**
 * 节点状态
 */
public enum State {
    /*
    领导者
     */
    LEADER,
    /*
    候选者
     */
    CANDIDATE,
    /*
    跟随者
     */
    FOLLOWER,
}
