package io.github.hligaty.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;

import java.util.WeakHashMap;

/**
 * @author hligaty
 */
public class BaseTest {
    
    @SneakyThrows
    protected void gc(int time) {
        WeakHashMap<WeakKey<BaseTest>, Object> map = new WeakHashMap<>();
        BaseTest test = new BaseTest();
        map.put(WeakKey.wrap(test), new Object());
        System.gc();
        Thread.sleep(time);
        Assertions.assertNull(map.get(WeakKey.wrap(test)), "jvm no gc");
    }

    @SneakyThrows
    protected void sleep(long timeout) {
        Thread.sleep(timeout);
    }
    
    @Data
    @AllArgsConstructor
    static class Username {
        private String username;
    }
}
