package io.github.hligaty.reference;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.util.WeakHashMap;

/**
 * @author hligaty
 */
public class BaseTest {
    
    @SneakyThrows
    protected void gc() {
        WeakHashMap<Username, Object> map = new WeakHashMap<>();
        int size = 0;
        while (map.size() == size) {
            map.put(new Username("sherry"), new Object());
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
