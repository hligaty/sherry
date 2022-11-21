package io.github.hligaty.util.concurrent.locks;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hligaty
 */
public class SherryLock<K> extends SLock<K, ReentrantLock> implements Lock {

    private SherryLock(K key) {
        super(new SherryLockKey<>(key));
    }

    public static <K> SherryLock<K> of(K key) {
        return new SherryLock<>(key);
    }

    @Override
    ReentrantLock newLock() {
        return new ReentrantLock();
    }

    static class SherryLockKey<K> implements LockKey {
        private final K key;

        public SherryLockKey(K key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SherryLockKey<?> that = (SherryLockKey<?>) o;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    @Override
    public void lock() {
        getLock().lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        getLock().lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return getLock().tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return getLock().tryLock(time, unit);
    }

    @Override
    public void unlock() {
        getLock().unlock();
    }

    @Override
    public Condition newCondition() {
        return getLock().newCondition();
    }
}
