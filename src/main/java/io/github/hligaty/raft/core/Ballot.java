package io.github.hligaty.raft.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 记录投票
 */
public class Ballot {

    private static final Logger LOG = LoggerFactory.getLogger(Ballot.class);
    
    private final CountDownLatch latch;
    
    private final String name;
    
    public Ballot(int count, String name) {
        this.latch = new CountDownLatch(count);
        this.name = name;
    }
    
    public void grant() {
        latch.countDown();
    }
    
    public boolean isGranted(long timeoutMs) {
        try {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error("等待{}完成时出现中断异常", name, e);
            return false;
        }
    }
}
