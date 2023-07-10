package io.github.hligaty.reference;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.MapMaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentMap;

/**
 * @author hligaty
 */
class GuavaTest extends BaseTest {

    @Test
    public void testGuavaInterner() {
        Interner<User> interner = Interners.newWeakInterner();
        User canonical = interner.intern(new User("shiho"));
        Assertions.assertSame(canonical, interner.intern(new User("shiho")));
    }

    @Test
    public void testGuavaInternerAfterGC() {
        Interner<User> interner = Interners.newWeakInterner();
        User shiho = new User("shiho");
        User not = new User("shiho");
        Assertions.assertSame(shiho, interner.intern(shiho));
//        gc(); // 执行 gc 将导致错误
        shiho = null;
        gc();
        Assertions.assertSame(not, interner.intern(not));
    }
    
    @Test
    public void testGuavaWeakKeyHashMap() {
        Interner<User> interner = Interners.newWeakInterner();
        ConcurrentMap<User, Object> map = new MapMaker().concurrencyLevel(1).weakKeys().makeMap();
        map.put(interner.intern(new User("shiho")), new Object());
        User haibara = new User("haibara");
        map.put(interner.intern(haibara), new Object());
        gc();
        Assertions.assertNull(map.get(interner.intern(new User("shiho"))));
        Assertions.assertNotNull(map.get(interner.intern(haibara)));
    }
}
