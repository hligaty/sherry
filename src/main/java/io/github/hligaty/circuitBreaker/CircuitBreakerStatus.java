package io.github.hligaty.circuitBreaker;

/**
 * 断路器状态
 *
 * @author hligaty
 * @date 2023/06/30
 */
public enum CircuitBreakerStatus {

    OPEN,
    CLOSED,
    HALF_OPEN,
}
