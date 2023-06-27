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
        Interner<Username> context = new Interner<>();
        WeakHashMap<Username, Object> map = new WeakHashMap<>();
        Username username = new Username("sherry");
        String s = username.toString();
        map.put(context.intern(username), new Object());
        username = null;
        System.gc();
        Thread.sleep(time);
        username = new Username("sherry");
        Assertions.assertEquals(username.toString(), s, "jvm no gc");
    }

    @SneakyThrows
    protected void sleep(long timeout) {
        Thread.sleep(timeout);
    }
    
    // @Data
    @AllArgsConstructor
    static class Username {
        private String username;
    }
}
