package io.github.hligaty.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Lock;

/**
 * @author hligaty
 */
class InfiniteStripedTest extends BaseTest {

    @Test
    public void testLock() throws InterruptedException {
        InfiniteStriped<Object, Lock> infiniteStriped = InfiniteStriped.lock();
        new Thread(() -> {
            Lock lock = infiniteStriped.get("lock");
            lock.lock();
            sleep(Long.MAX_VALUE);
        }).start();
        Thread thread = new Thread(() -> {
            sleep(5000);
            Lock lock = infiniteStriped.get("lock");
            Assertions.assertFalse(lock.tryLock());
        });
        thread.start();
        gc(3000);
        thread.join();
    }
}
