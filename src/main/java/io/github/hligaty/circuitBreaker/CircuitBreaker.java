package io.github.hligaty.circuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * 简单的断路器
 */
public class CircuitBreaker {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreaker.class);
    /**
     * 失败阈值
     */
    private final long failureThreshold;
    /**
     * 熔断开启时间
     */
    private final long waitDurationInOpenState;
    /**
     * 函数执行器
     */
    private final ExecutorService executor =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("circuit-breaker").factory());
    /**
     * 函数执行超时时间
     */
    private final long waitDurationInExecute;
    /**
     * 失败计数
     */
    private final LongAdder failureCount = new LongAdder();
    /**
     * 断路器状态
     */
    private final AtomicReference<CircuitBreakerStatus> status =
            new AtomicReference<>(CircuitBreakerStatus.CLOSED);
    /**
     * 熔断结束时间
     */
    private long endTime = -1L;

    public CircuitBreaker(long failureThreshold, Duration waitDurationInOpenState, Duration waitDurationInExecute) {
        this.failureThreshold = failureThreshold;
        this.waitDurationInOpenState = waitDurationInOpenState.toMillis();
        this.waitDurationInExecute = waitDurationInExecute.toMillis();
    }

    /**
     * 执行函数
     *
     * @param callable 函数
     * @param <V>      返回值类型
     * @return 返回值
     */
    public <V> V executeSupplier(Callable<V> callable) {
        tryAcquire();
        try {
            Future<V> future = executor.submit(callable);
            V v = future.get(waitDurationInExecute, TimeUnit.MILLISECONDS);
            successCallback();
            return v;
        } catch (Exception e) {
            failureCallback();
            if (e instanceof RuntimeException exception) {
                throw exception;
            }
            throw new RuntimeException(e);
        }
    }

    private void tryAcquire() {
        // 断路器半开以及关闭状态都可以请求
        if (CircuitBreakerStatus.OPEN.equals(status.get())) {
            // 只有在开启时需要判断熔断时间
            if (endTime >= System.currentTimeMillis()) {
                throw new CircuitBreakerException();
            }
            // 渡过了熔断时间, 恢复到半开状态
            changeStatus(CircuitBreakerStatus.OPEN, CircuitBreakerStatus.HALF_OPEN);
        }
    }

    private void successCallback() {
        // 尝试将半开状态转换为关闭
        if (changeStatus(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.CLOSED)) {
            failureCount.reset();
        }
    }

    private void failureCallback() {
        if (changeStatus(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.OPEN)) {
            endTime = System.currentTimeMillis() + waitDurationInOpenState;
            return;
        }
        failureCount.increment();
        if (failureCount.sum() >= failureThreshold
                && changeStatus(CircuitBreakerStatus.CLOSED, CircuitBreakerStatus.OPEN)) {
            endTime = System.currentTimeMillis() + waitDurationInOpenState;
        }
    }

    private boolean changeStatus(CircuitBreakerStatus oldStatus, CircuitBreakerStatus newStatus) {
        return status.compareAndSet(oldStatus, newStatus);
    }
}
