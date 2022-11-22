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
class InternerTest extends BaseTest {

    @Test
    public void testWeakKey() throws InterruptedException {
        Interner<List<Integer>> interner = new Interner<>();
        WeakHashMap<List<Integer>, Object> map = new WeakHashMap<>();
        map.put(interner.intern(Arrays.asList(705, 630, 818)), new Object());
        List<Integer> weakObject = interner.intern(Collections.singletonList(705630818));
        map.put(weakObject, new Object());
        System.gc();
        Thread.sleep(2000);
        Assertions.assertNull(map.get(interner.intern(Arrays.asList(705, 630, 818))));
        Assertions.assertNotNull(map.get(interner.intern(Collections.singletonList(705630818))));
    }

    @Test
    public void testSynchronized() throws InterruptedException {
        Interner<String> context = new Interner<>();
        Thread thread = new Thread(() -> {
            synchronized (context.intern("sherry")) {
                sleep(3000);
                synchronized (context.intern("haibara")) {
                    Assertions.fail();
                }
            }
        });
        thread.start();
        new Thread(() -> {
            synchronized (context.intern("haibara")) {
                sleep(3000);
                synchronized (context.intern("sherry")) {
                    Assertions.fail();
                }
            }
        }).start();
        gc(2000);
        thread.join();
    }
}
