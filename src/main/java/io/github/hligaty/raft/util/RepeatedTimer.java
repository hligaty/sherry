package io.github.hligaty.raft.util;

import com.alipay.remoting.NamedThreadFactory;
import com.google.common.base.CaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 重复不定时执行任务的定时器.
 */
public abstract class RepeatedTimer {

    private static final Logger LOG = LoggerFactory.getLogger(NamedThreadFactory.class);

    private final ScheduledExecutorService scheduledExecutorService;

    private final Lock lock = new ReentrantLock();

    private boolean stopped = true;

    private ScheduledFuture<?> scheduledFuture;


    public RepeatedTimer(String name) {
        ThreadFactory threadFactory = Thread.ofVirtual().name(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name) + "-thread").factory();
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1, threadFactory);
    }

    protected abstract int adjustTimeout();

    protected abstract void onTrigger();

    public final void start() {
        lock.lock();
        try {
            if (stopped) {
                stopped = false;
                schedule();
            }
        } finally {
            lock.unlock();
        }
    }

    private void schedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = scheduledExecutorService.schedule(this::run, adjustTimeout(), TimeUnit.MILLISECONDS);
    }

    public final void stop() {
        lock.lock();
        try {
            stopped = true;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
        } finally {
            lock.unlock();
        }
    }

    private void run() {
        Tracker.start();
        try {
            onTrigger();
        } catch (Throwable throwable) {
            LOG.error("Uncaught exception in thread {}", Thread.currentThread(), throwable);
        } finally {
            Tracker.stop();
            lock.lock();
            try {
                if (!stopped) {
                    scheduledFuture = null;
                    schedule();
                }
            } finally {
                lock.unlock();
            }
        }
    }

}
