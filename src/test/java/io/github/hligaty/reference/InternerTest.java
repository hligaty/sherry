package io.github.hligaty.reference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

/**
 * @author hligaty
 */
class InternerTest extends BaseTest {

    @Test
    public void test() {
        Interner<List<Integer>> interner = new Interner<>();
        WeakHashMap<List<Integer>, Object> map = new WeakHashMap<>();
        map.put(interner.intern(Arrays.asList(705, 630, 818)), new Object());
        List<Integer> weakObject = interner.intern(Collections.singletonList(705630818));
        map.put(weakObject, new Object());
        gc();
        Assertions.assertNull(map.get(interner.intern(Arrays.asList(705, 630, 818))));
        Assertions.assertNotNull(map.get(interner.intern(Collections.singletonList(705630818))));
    }
}
