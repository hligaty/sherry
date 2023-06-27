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
        InfiniteStriped<Lock> infiniteStriped = InfiniteStriped.lock();
        new Thread(() -> {
            Lock lock = infiniteStriped.get("haibara");
            lock.lock();
            sleep(Long.MAX_VALUE);
        }).start();
        Thread thread = new Thread(() -> {
            sleep(5000);
            Lock lock = infiniteStriped.get("haibara");
            Assertions.assertFalse(lock.tryLock());
        });
        thread.start();
        gc();
        thread.join();
    }
}
