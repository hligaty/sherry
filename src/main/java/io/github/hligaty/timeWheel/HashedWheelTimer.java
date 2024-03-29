package io.github.hligaty.timeWheel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 多层时间轮
 */
public class HashedWheelTimer implements Timer {

    private static final Logger LOG = LoggerFactory.getLogger(HashedWheelTimer.class);

    private final long tickDuration;

    private final long startTime;

    private final Worker worker = new Worker();

    private final Queue<HashedWheelTimeout> tasks = new LinkedBlockingQueue<>();

    private final HashedWheelBucket[] wheel;

    public HashedWheelTimer() {
        this(Duration.ofMillis(100), 512);
    }

    public HashedWheelTimer(Duration tickDuration, int ticksPerWheel) {
        this.startTime = System.nanoTime();
        this.tickDuration = tickDuration.toNanos();
        this.wheel = IntStream.range(0, ticksPerWheel)
                .mapToObj(__ -> new HashedWheelBucket())
                .toArray(HashedWheelBucket[]::new);
        Thread.ofPlatform().name("hashed-wheel-timer-thread").start(worker);
    }

    @Override
    public void newTimeout(Runnable task, Duration delay) {
        long deadline = System.nanoTime() - startTime + delay.toNanos();
        tasks.add(new HashedWheelTimeout(task, deadline));
    }

    @Override
    public Set<Runnable> stop() {
        return worker.stop();
    }

    private class HashedWheelBucket {

        private final List<HashedWheelTimeout> timeouts = new LinkedList<>();

        public void addTimeout(HashedWheelTimeout timeout) {
            timeouts.add(timeout);
        }

        public void expireTimeouts() {
            long tick = worker.tick;
            Iterator<HashedWheelTimeout> iterator = timeouts.iterator();
            while (iterator.hasNext()) {
                HashedWheelTimeout timeout = iterator.next();
                if (timeout.remainingTicks > tick) {
                    continue;
                }
                iterator.remove();
                // 最好将真正执行的线程与当前线程隔离开, 当然如果是 Netty 这种响应式的话也就无所谓了
                timeout.expire();
            }
        }

    }

    private class Worker implements Runnable {

        private Thread workerThread;

        private Thread mainThread;

        private long tick;
        
        private Set<Runnable> unprocessedTasks = Collections.emptySet();

        @Override
        public void run() {
            workerThread = Thread.currentThread();
            while (waitForNextTick()) {
                transferTasksToBuckets();
                HashedWheelBucket bucket = wheel[(int) (tick % wheel.length)];
                bucket.expireTimeouts();
                tick++;
            }
            unprocessedTasks = Stream.of(wheel)
                    .flatMap(bucket -> bucket.timeouts.stream())
                    .map(timeout -> timeout.task)
                    .collect(Collectors.toUnmodifiableSet());
            LockSupport.unpark(mainThread);
        }

        private boolean waitForNextTick() {
            long deadline = (tick + 1) * tickDuration;
            long currentTime = System.nanoTime() - startTime;
            long sleepTimeNanos = deadline - currentTime;
            // 因为阻塞时间不准(Windows Thread.sleep(1) 最低是 10ms), 所以按照截止时间计算睡眠多久, 否则执行时间会偏差的越来越长.
            LockSupport.parkNanos(sleepTimeNanos);
            return !Thread.interrupted();
        }

        private void transferTasksToBuckets() {
            HashedWheelTimeout timeout;
            while ((timeout = tasks.poll()) != null) { // 不考虑转移任务耗时太长需要限制数量的问题
                // 这里直接用轮次判断了, 隐藏了多层的概念, 相对简单点
                timeout.remainingTicks = timeout.deadline / tickDuration;
                // 和 Netty DefaultEventLoop 定时任务一个思路, 已经超时了要立即执行, 而不是过了一轮后才执行
                int stopIndex = (int) (Math.max(timeout.remainingTicks, tick) % wheel.length);
                wheel[stopIndex].addTimeout(timeout);
            }
        }

        public Set<Runnable> stop() {
            mainThread = Thread.currentThread();
            workerThread.interrupt();
            LockSupport.park(mainThread);
            return unprocessedTasks;
        }
    }

    private static class HashedWheelTimeout {

        private final Runnable task;

        private final long deadline;

        private long remainingTicks;

        public HashedWheelTimeout(Runnable task, long deadline) {
            this.task = task;
            this.deadline = deadline;
        }
        
        public void expire() {
            try {
                task.run();
            } catch (Throwable throwable) {
                LOG.info("An exception was thrown by {}.", HashedWheelTimer.class.getSimpleName(), throwable);
            }
        }
        
    }

}
