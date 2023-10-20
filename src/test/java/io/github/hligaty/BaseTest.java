package io.github.hligaty;

import java.util.WeakHashMap;

/**
 * @author hligaty
 */
public class BaseTest {

    protected static void gc() {
        WeakHashMap<Object, Object> map = new WeakHashMap<>();
        int size = 0;
        while (map.size() == size++) {
            map.put(new Object(), new Object());
        }
    }

    protected static void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
}
