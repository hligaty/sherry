package io.github.hligaty.reference;

import com.google.common.collect.Interners;
import com.google.common.collect.Interner;
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
        User user = new User("shiho");
        /*
        为什么此处执行 gc() 会导致 interner 中的 value 没有被 gc 掉导致第二次断言异常
        此处执行 gc 好像应该不会有任何影响才对
         */
        Assertions.assertSame(user, interner.intern(user));
        user = new User("shiho");
        gc();
        Assertions.assertSame(user, interner.intern(user));
    }
    
    @Test
    public void testGuavaWeakKeyHashMap() {
        Interner<User> interner = Interners.newWeakInterner();
        ConcurrentMap<User, Object> map = new MapMaker().concurrencyLevel(1).weakKeys().makeMap();
        map.put(interner.intern(new User("shiho")), new Object());
        User sherry = new User("haibara");
        map.put(interner.intern(sherry), new Object());
        gc();
        Assertions.assertNull(map.get(interner.intern(new User("shiho"))));
        Assertions.assertNotNull(map.get(interner.intern(new User("haibara"))));
    }
}
