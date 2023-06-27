package io.github.hligaty.util;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * @author hligaty
 */
public class BaseTest {
    
    @SneakyThrows
    protected void gc() {
        WeakHashMap<WeakReference<Username>, Object> map = new WeakHashMap<>();
        int size = 0;
        while (map.size() == size) {
            map.put(new WeakReference<>(new Username("sherry")), new Object());
            size++;
        }
    }

    @SneakyThrows
    protected void sleep(long timeout) {
        Thread.sleep(timeout);
    }
    
    @AllArgsConstructor
    static class Username {
        private String username;
    }
}
