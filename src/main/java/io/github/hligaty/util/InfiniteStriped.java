package io.github.hligaty.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/**
 * Generate a lock for each object equal to equals () separately, and the memory is safe.
 * The usage is similar to Guava Striped, but objects with the same hashcode will not compete for the same lock.
 *
 * @param <T> the type of lock maintained
 * @author hligaty
 */
public abstract class InfiniteStriped<T> {
    
    private InfiniteStriped() {
    }
    
    public abstract T get(Object key);

    public static InfiniteStriped<Lock> lock() {
        WeakSafeContext<SLock<Lock>> context = new WeakSafeContext<>();
        return new InfiniteStriped<Lock>() {
            @Override
            public Lock get(Object key) {
                return new SherryWeakSafeLock(key, context);
            }
        };
    }

    public static InfiniteStriped<ReadWriteLock> readWriteLock() {
        WeakSafeContext<SLock<ReadWriteLock>> context = new WeakSafeContext<>();
        return new InfiniteStriped<ReadWriteLock>() {
            @Override
            public ReadWriteLock get(Object key) {
                return new SherryWeakSafeReadWriteLock(key, context);
            }
        };
    }

    private static abstract class SLock<T> {
        private final Object key;
        private volatile T lock;

        private final WeakSafeContext.WeakObject<SLock<T>> weakObject;

        protected SLock(Object key, WeakSafeContext<SLock<T>> context) {
            this.key = key;
            this.weakObject = context.wrap(this);
        }

        public T getLock() {
            SLock<T> sLock = weakObject.unwrap();
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
            SLock<?> sLock = (SLock<?>) o;
            return Objects.equals(key, sLock.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    private static class SherryWeakSafeLock extends SLock<Lock> implements Lock {

        SherryWeakSafeLock(Object key, WeakSafeContext<SLock<Lock>> context) {
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

    private static class SherryWeakSafeReadWriteLock extends SLock<ReadWriteLock> implements ReadWriteLock {

        public SherryWeakSafeReadWriteLock(Object key, WeakSafeContext<SLock<ReadWriteLock>> context) {
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
