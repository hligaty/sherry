package io.github.hligaty.raft.core;

/**
 * 客户端命令执行失败类型
 */
public enum ErrorType {
    /*
    不是领导者
     */
    NOT_LEADER,
    /*
    日志复制到大多数失败
     */
    REPLICATION_FAIL,
    /*
    状态机执行失败
     */
    STATE_MACHINE_FAIL,
}
