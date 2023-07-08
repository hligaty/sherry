package io.github.hligaty.reference;

import java.util.WeakHashMap;

/**
 * @author hligaty
 */
public class BaseTest {
    
    protected void gc() {
        WeakHashMap<Object, Object> map = new WeakHashMap<>();
        int size = 0;
        while (map.size() == size++) {
            map.put(new Object(), new Object());
        }
    }

    protected void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    record User(String name) {
    }
}
