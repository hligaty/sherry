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
        WeakSafeContext<BaseTest> context = new WeakSafeContext<>();
        WeakHashMap<WeakSafeContext.WeakObject<BaseTest>, Object> map = new WeakHashMap<>();
        BaseTest test = new BaseTest();
        map.put(context.wrap(test), new Object());
        System.gc();
        Thread.sleep(time);
        Assertions.assertNull(map.get(context.wrap(test)), "jvm no gc");
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
