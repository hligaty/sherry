package io.github.hligaty.util.concurrent.locks;

import io.github.hligaty.util.WeakKey;

import java.util.Objects;

/**
 * Object lock.
 * Using equals() as the condition of variable equality, memory security
 *
 * @param <K> the type of keys maintained
 * @param <T> the type of lock maintained
 * @author hligaty
 * @see SherryLock
 * @see SherryReadWriteLock
 */
public abstract class SLock<K, T> {
    private final LockKey key;
    private volatile T lock;

    private final WeakKey<SLock<K, T>> weakKey;

    protected SLock(LockKey key) {
        this.key = key;
        this.weakKey = WeakKey.wrap(this);
    }

    public T getLock() {
        SLock<K, T> sLock = weakKey.unwrap();
        if (sLock.lock == null) {
            synchronized (this) {
                if (sLock.lock == null) {
                    sLock.lock = newLock();
                }
            }
        }
        return sLock.lock;
    }

    abstract T newLock();

    interface LockKey {

        boolean equals(Object o);

        int hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SLock<?, ?> sLock = (SLock<?, ?>) o;
        return Objects.equals(key, sLock.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
