package io.github.hligaty.util.concurrent.locks;

import io.github.hligaty.util.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Lock;

/**
 * @author hligaty
 */
class SherryLockTest extends BaseTest {

    @Test
    public void testLock() throws InterruptedException {
        new Thread(() -> {
            Lock lock = SherryLock.of("lock");
            lock.lock();
            sleep(Long.MAX_VALUE);
        }).start();
        Thread thread = new Thread(() -> {
            sleep(5000);
            Lock lock = SherryLock.of("lock");
            Assertions.assertFalse(lock.tryLock());
        });
        thread.start();
        gc(3000);
        thread.join();
    }
}
