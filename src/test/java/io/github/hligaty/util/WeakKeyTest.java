package io.github.hligaty.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

/**
 * @author hligaty
 */
class WeakKeyTest extends BaseTest {

    @Test
    public void testWeakKey() throws InterruptedException {
        WeakHashMap<WeakKey<List<Integer>>, Object> map = new WeakHashMap<>();
        map.put(WeakKey.wrap(Arrays.asList(705, 630, 818)), new Object());
        WeakKey<List<Integer>> weakKey = WeakKey.wrap(Collections.singletonList(705630818));
        map.put(weakKey, new Object());
        System.gc();
        Thread.sleep(2000);
        Assertions.assertNull(map.get(WeakKey.wrap(Arrays.asList(705, 630, 818))));
        Assertions.assertNotNull(map.get(WeakKey.wrap(Collections.singletonList(705630818))));
    }

    @Test
    public void testSynchronized() throws InterruptedException {
        Thread thread = new Thread(() -> {
            synchronized (WeakKey.wrap("sherry")) {
                sleep(3000);
                synchronized (WeakKey.wrap("haibara")) {
                    Assertions.fail();
                }
            }
        });
        thread.start();
        new Thread(() -> {
            synchronized (WeakKey.wrap("haibara")) {
                sleep(3000);
                synchronized (WeakKey.wrap("sherry")) {
                    Assertions.fail();
                }
            }
        }).start();
        gc(2000);
        thread.join();
    }
}
