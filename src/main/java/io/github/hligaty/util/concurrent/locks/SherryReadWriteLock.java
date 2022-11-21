package io.github.hligaty.util.concurrent.locks;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Object readWriteLock
 *
 * @param <K> the type of keys maintained
 * @author hligaty
 */
public class SherryReadWriteLock<K> extends SLock<K, ReadWriteLock> implements ReadWriteLock {

    private SherryReadWriteLock(K key) {
        super(new SherryReadWriteLockKey<>(key));
    }

    public static <K> SherryReadWriteLock<K> of(K key) {
        return new SherryReadWriteLock<>(key);
    }

    @Override
    ReadWriteLock newLock() {
        return new ReentrantReadWriteLock();
    }

    static class SherryReadWriteLockKey<K> implements LockKey {
        private final K key;

        public SherryReadWriteLockKey(K key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SherryReadWriteLockKey<?> that = (SherryReadWriteLockKey<?>) o;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    @Override
    public Lock readLock() {
        return getLock().readLock();
    }

    @Override
    public Lock writeLock() {
        return getLock().writeLock();
    }
}
