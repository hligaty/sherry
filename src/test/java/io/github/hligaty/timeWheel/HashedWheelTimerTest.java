package io.github.hligaty.timeWheel;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HashedWheelTimerTest {

    @Test
    void test() throws InterruptedException {
        Timer timer = new HashedWheelTimer();
        List<String> expected = List.of("0.4", "0.6", "0.8", "0.9", "4", "6", "8", "9");
        List<String> actual = new CopyOnWriteArrayList<>();
        timer.newTimeout(() -> actual.add("4"), Duration.ofSeconds(4));
        timer.newTimeout(() -> actual.add("8"), Duration.ofSeconds(8));
        timer.newTimeout(() -> actual.add("6"), Duration.ofSeconds(6));
        timer.newTimeout(() -> actual.add("9"), Duration.ofSeconds(9));
        timer.newTimeout(() -> actual.add("0.4"), Duration.ofMillis(600));
        timer.newTimeout(() -> actual.add("0.8"), Duration.ofMillis(800));
        timer.newTimeout(() -> actual.add("0.6"), Duration.ofMillis(600));
        timer.newTimeout(() -> actual.add("0.9"), Duration.ofMillis(900));
        TimeUnit.SECONDS.sleep(11);
        assertEquals(0, timer.stop().size());
        assertArrayEquals(expected.toArray(), actual.toArray());
    }
}
