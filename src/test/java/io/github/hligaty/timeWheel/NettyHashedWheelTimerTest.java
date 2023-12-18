package io.github.hligaty.timeWheel;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NettyHashedWheelTimerTest {

    @Test
    void test() throws InterruptedException {
        Timer timer = new HashedWheelTimer();
        List<String> expected = List.of("0.4", "0.6", "0.8", "0.9", "4", "6", "8", "9");
        List<String> actual = new CopyOnWriteArrayList<>();
        long l = System.currentTimeMillis();
        timer.newTimeout(__ -> {
            System.out.println("4 " + (System.currentTimeMillis() - l));
            actual.add("4");
        }, 4, TimeUnit.SECONDS);
        timer.newTimeout(__ -> {
            System.out.println("8 " + (System.currentTimeMillis() - l));
            actual.add("8");
        }, 8, TimeUnit.SECONDS);
        timer.newTimeout(__ -> {
            System.out.println("6 " + (System.currentTimeMillis() - l));
            actual.add("6");
        }, 6, TimeUnit.SECONDS);
        timer.newTimeout(__ -> {
            System.out.println("9 " + (System.currentTimeMillis() - l));
            actual.add("9");
        }, 9, TimeUnit.SECONDS);
        timer.newTimeout(__ -> {
            System.out.println("0.4 " + (System.currentTimeMillis() - l));
            actual.add("0.4");
        }, 600, TimeUnit.MILLISECONDS);
        timer.newTimeout(__ -> {
            System.out.println("0.8 " + (System.currentTimeMillis() - l));
            actual.add("0.8");
        }, 800, TimeUnit.MILLISECONDS);
        timer.newTimeout(__ -> {
            System.out.println("0.6 " + (System.currentTimeMillis() - l));
            actual.add("0.6");
        }, 600, TimeUnit.MILLISECONDS);
        timer.newTimeout(__ -> {
            System.out.println("0.9 " + (System.currentTimeMillis() - l));
            actual.add("0.9");
        }, 900, TimeUnit.MILLISECONDS);
        TimeUnit.SECONDS.sleep(10);
        assertEquals(0, timer.stop().size());
        assertArrayEquals(expected.toArray(), actual.toArray());
    }
}
