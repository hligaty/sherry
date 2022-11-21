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
class WeakSafeContextTest extends BaseTest {

    @Test
    public void testWeakKey() throws InterruptedException {
        WeakSafeContext<List<Integer>> context = new WeakSafeContext<>();
        WeakHashMap<WeakSafeContext.WeakObject<List<Integer>>, Object> map = new WeakHashMap<>();
        map.put(context.wrap(Arrays.asList(705, 630, 818)), new Object());
        WeakSafeContext.WeakObject<List<Integer>> weakObject = context.wrap(Collections.singletonList(705630818));
        map.put(weakObject, new Object());
        System.gc();
        Thread.sleep(2000);
        Assertions.assertNull(map.get(context.wrap(Arrays.asList(705, 630, 818))));
        Assertions.assertNotNull(map.get(context.wrap(Collections.singletonList(705630818))));
    }

    @Test
    public void testSynchronized() throws InterruptedException {
        WeakSafeContext<String> context = new WeakSafeContext<>();
        Thread thread = new Thread(() -> {
            synchronized (context.wrap("sherry")) {
                sleep(3000);
                synchronized (context.wrap("haibara")) {
                    Assertions.fail();
                }
            }
        });
        thread.start();
        new Thread(() -> {
            synchronized (context.wrap("haibara")) {
                sleep(3000);
                synchronized (context.wrap("sherry")) {
                    Assertions.fail();
                }
            }
        }).start();
        gc(2000);
        thread.join();
    }
}
