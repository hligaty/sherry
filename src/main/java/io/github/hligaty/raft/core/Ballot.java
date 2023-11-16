package io.github.hligaty.raft.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Ballot {

    private static final Logger LOG = LoggerFactory.getLogger(Ballot.class);
    
    private final CountDownLatch latch;
    
    private final boolean preVote;
    
    public Ballot(int count, boolean preVote) {
        this.latch = new CountDownLatch(count);
        this.preVote = preVote;
    }
    
    public void grant() {
        latch.countDown();
    }
    
    public boolean await(long timeout, TimeUnit unit) {
        try {
            return latch.await(timeout, unit);
        } catch (InterruptedException e) {
            LOG.error("等待{}完成时出现中断异常", preVote ? "预投票" : "正式投票", e);
            return false;
        }
    }
}
