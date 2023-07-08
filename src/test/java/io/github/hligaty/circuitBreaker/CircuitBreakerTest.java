package io.github.hligaty.circuitBreaker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

class CircuitBreakerTest {

    @Test
    public void test() throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        CircuitBreaker circuitBreaker = new CircuitBreaker(5, Duration.ofMillis(100));
        for (int i = 0; i < 5; i++) {
            circuitBreaker.executeSupplier(count::incrementAndGet);
        }
        // circuitBreaker closed
        for (int i = 0; i < 5; i++) {
            Exception exception = null;
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException();
                });
            } catch (Exception e) {
                exception = e;
            }
            Assertions.assertNotNull(exception);
        }
        // circuitBreaker open
        Thread.sleep(101L);
        // circuitBreaker half open
        Integer temp = circuitBreaker.executeSupplier(count::incrementAndGet);
        // circuitBreaker closed
        Assertions.assertEquals(6, temp);
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException();
                });
            } catch (Exception ignored) {
            }
        }
        // circuitBreaker open
        CircuitBreakerException exception = null;
        try {
            circuitBreaker.executeSupplier(count::incrementAndGet);
        } catch (CircuitBreakerException e) {
            exception = e;
        }
        Assertions.assertNotNull(exception);
    }
    
}
