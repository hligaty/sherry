package io.github.hligaty.reference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author hligaty
 */
class InternerTest extends BaseTest {

    @Test
    public void test() {
        Interner<List<Integer>> interner = new Interner<>();
        List<Integer> intern = interner.intern(Arrays.asList(705, 630, 818));
        Assertions.assertSame(intern, interner.intern(Arrays.asList(705, 630, 818)));
    }
}
