package io.github.hligaty.reference;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.MapMaker;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * @author hligaty
 */
class GuavaTest extends BaseTest {

    @Test
    public void testGuavaInterner() {
        Interner<ID> interner = Interners.newWeakInterner();
        ID id = interner.intern(new ID("shiho"));
        String toString = id.toString();
        gc();
        Assertions.assertSame(id, interner.intern(new ID("shiho")));
        id = null;
        gc();
        Assertions.assertNotEquals(toString, interner.intern(new ID("shiho")).toString());
    }

    @Test
    public void testGuavaWeakHashMap() {
        Interner<ID> interner = Interners.newWeakInterner();
        ConcurrentMap<ID, Object> map = new MapMaker().concurrencyLevel(1).weakKeys().makeMap();
        // new/threadLocal/objectPool
        map.put(interner.intern(new ID("shiho")), new Object());
        ID sherry = new ID("haibara");
        map.put(interner.intern(sherry), new Object());
        gc();
        Assertions.assertNull(map.get(interner.intern(new ID("shiho"))));
        Assertions.assertNotNull(map.get(interner.intern(new ID("haibara"))));
    }

    @Data
    static class ID {
        private final String id;

        @Override
        public String toString() {
            return UUID.randomUUID().toString();
        }
    }
}
