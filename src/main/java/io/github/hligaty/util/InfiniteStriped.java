package io.github.hligaty.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/**
 * Generate a lock for each object equal to equals () separately, and the memory is safe.
 * The usage is similar to Guava Striped, but objects with the same hashcode will not compete for the same lock.
 *
 * @param <K> the type of keys maintained
 * @param <T> the type of lock maintained
 * @author hligaty
 */
public abstract class InfiniteStriped<K, T> {
    
    private InfiniteStriped() {
    }
    
    public abstract T get(K key);

    public static <K> InfiniteStriped<K, Lock> lock() {
        WeakSafeContext<SLock<K, Lock>> context = new WeakSafeContext<>();
        return new InfiniteStriped<K, Lock>() {
            @Override
            public Lock get(K key) {
                return new SherryWeakSafeLock<>(key, context);
            }
        };
    }

    public static <K> InfiniteStriped<K, ReadWriteLock> readWriteLock() {
        WeakSafeContext<SLock<K, ReadWriteLock>> context = new WeakSafeContext<>();
        return new InfiniteStriped<K, ReadWriteLock>() {
            @Override
            public ReadWriteLock get(K key) {
                return new SherryWeakSafeReadWriteLock<>(key, context);
            }
        };
    }

    private static abstract class SLock<K, T> {
        private final K key;
        private volatile T lock;

        private final WeakSafeContext.WeakObject<SLock<K, T>> weakObject;

        protected SLock(K key, WeakSafeContext<SLock<K, T>> context) {
            this.key = key;
            this.weakObject = context.wrap(this);
        }

        public T getLock() {
            SLock<K, T> sLock = weakObject.unwrap();
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

    private static class SherryWeakSafeLock<K> extends SLock<K, Lock> implements Lock {

        SherryWeakSafeLock(K key, WeakSafeContext<SLock<K, Lock>> context) {
            super(key, context);
        }

        @Override
        ReentrantLock newLock() {
            return new ReentrantLock();
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

    private static class SherryWeakSafeReadWriteLock<K> extends SLock<K, ReadWriteLock> implements ReadWriteLock {

        public SherryWeakSafeReadWriteLock(K key, WeakSafeContext<SLock<K, ReadWriteLock>> context) {
            super(key, context);
        }

        @Override
        ReadWriteLock newLock() {
            return new ReentrantReadWriteLock();
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

}
