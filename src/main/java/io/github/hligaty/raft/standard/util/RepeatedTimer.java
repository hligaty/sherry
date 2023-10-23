package io.github.hligaty.raft.standard.util;

import com.alipay.remoting.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class RepeatedTimer {

    private static final Logger LOG = LoggerFactory.getLogger(NamedThreadFactory.class);

    private final ScheduledExecutorService scheduledExecutorService;

    public RepeatedTimer(String name) {
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {

            @Override
            public Thread newThread(@Nonnull Runnable r) {
                Thread thread = new Thread(r, name);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    protected abstract int adjustTimeout();

    protected abstract void onTrigger();

    public final void start() {
        scheduledExecutorService.schedule(this::run, adjustTimeout(), TimeUnit.MILLISECONDS);
    }

    private void run() {
        try {
            onTrigger();
        } catch (Throwable throwable) {
            LOG.error("Uncaught exception in thread {}", Thread.currentThread(), throwable);
        } finally {
            start();
        }
    }
}
