package io.github.hligaty.raft.util;

public class Peer {

    private final PeerId id;

    /**
     * 当前节点为领导者时使用, 记录要发送给这个跟随者的下一个日志
     */
    private long nextIndex;

    public Peer(PeerId id) {
        this.id = id;
    }

    public PeerId id() {
        return id;
    }

    public long nextIndex() {
        return nextIndex;
    }

    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex;
    }
}
