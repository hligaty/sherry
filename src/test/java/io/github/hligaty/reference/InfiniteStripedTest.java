package io.github.hligaty.reference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Lock;

/**
 * @author hligaty
 */
class InfiniteStripedTest extends BaseTest {

    @Test
    public void testLock() {
        InfiniteStriped<Lock> infiniteStriped = InfiniteStriped.lock();
        Thread thread = new Thread(() -> {
            Lock lock = infiniteStriped.get(new User("haibara"));
            lock.lock();
            sleep(Long.MAX_VALUE);
        });
        thread.start();
        while (true) {
            if (thread.isAlive()) {
                break;
            }
        }
        gc();
        Lock lock = infiniteStriped.get(new User("haibara"));
        Assertions.assertFalse(lock.tryLock());
    }
}
