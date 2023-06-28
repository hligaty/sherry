package io.github.hligaty.circuitBreaker;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

class CircuitBreakerTest {

    @Test
    @SneakyThrows
    public void test() {
        AtomicInteger count = new AtomicInteger();
        CircuitBreaker circuitBreaker = new CircuitBreaker(5, 100);
        for (int i = 0; i < 5; i++) {
            circuitBreaker.executeSupplier(count::incrementAndGet);
        }
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
        Thread.sleep(101L);
        Integer temp = circuitBreaker.executeSupplier(count::incrementAndGet);
        Assertions.assertEquals(6, temp);
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException();
                });
            } catch (Exception ignored) {
            }
        }
        CircuitBreakerException exception = null;
        try {
            circuitBreaker.executeSupplier(count::incrementAndGet);
        } catch (CircuitBreakerException e) {
            exception = e;
        }
        Assertions.assertNotNull(exception);
    }

}
