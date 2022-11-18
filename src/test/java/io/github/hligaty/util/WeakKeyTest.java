package io.github.hligaty.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

class WeakKeyTest {
    
    @Test
    public void testWeakKey() throws InterruptedException {
        WeakHashMap<WeakKey<List<Integer>>, Object> map = new WeakHashMap<>();
        map.put(WeakKey.wrap(Arrays.asList(705, 630, 818)), new Object());
        WeakKey<List<Integer>> weakKey = WeakKey.wrap(Collections.singletonList(705630818));
        map.put(weakKey, new Object());
        System.gc();
        Thread.sleep(5000L);
        Assertions.assertNull(map.get(WeakKey.wrap(Arrays.asList(705, 630, 818))));
        Assertions.assertNotNull(map.get(WeakKey.wrap(Collections.singletonList(705630818))));
    }
}
