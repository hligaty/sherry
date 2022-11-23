package io.github.hligaty.util;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.MapMaker;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentMap;

/**
 * @author hligaty
 */
class GuavaTest extends BaseTest {

    @Test
    public void testGuavaInterner() {
        Interner<ID> interner = Interners.newWeakInterner();
        ID id = interner.intern(new ID("1"));
        String toString = id.toString();
        gc(2000);
        Assertions.assertSame(id, interner.intern(new ID("1")));
        id = null;
        gc(2000);
        Assertions.assertNotEquals(toString, interner.intern(new ID("1")).toString());
    }
    
    @Test
    public void testGuavaWeakHashMap() {
        Interner<ID> interner = Interners.newWeakInterner();
        ConcurrentMap<ID, Object> map = new MapMaker().concurrencyLevel(1).weakKeys().makeMap();
        // new/threadLocal/objectPool
        map.put(interner.intern(new ID("1")), new Object());
        gc(2000);
        Assertions.assertNull(map.get(interner.intern(new ID("1"))));
    }
    
    @Data
    static class ID {
        private final String id;

        @Override
        public String toString() {
            return String.valueOf(super.hashCode());
        }
    }
}
