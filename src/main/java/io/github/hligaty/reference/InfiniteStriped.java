package io.github.hligaty.reference;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 分别为每个等于equals（）的对象生成一个锁，并且内存是安全的。其用法类似于Guava Striped，但具有相同哈希代码的对象不会争夺相同的锁
 * @param <T> 维护的锁类型
 * @author hligaty
 * @date 2022/11/21
 */
public interface InfiniteStriped<T> {

    T get(Object key);

    static InfiniteStriped<Lock> lock() {
        Interner<SLock<Lock>> context = new Interner<>();
        return key -> new SherryWeakSafeLock(key, context);
    }

    static InfiniteStriped<ReadWriteLock> readWriteLock() {
        Interner<SLock<ReadWriteLock>> context = new Interner<>();
        return key -> new SherryWeakSafeReadWriteLock(key, context);
    }

    abstract class SLock<T> {
        private final Object key;
        private volatile T lock;

        private final SLock<T> sLock;

        protected SLock(Object key, Interner<SLock<T>> interner) {
            this.key = key;
            this.sLock = interner.intern(this);
        }

        public T getLock() {
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

    class SherryWeakSafeLock extends SLock<Lock> implements Lock {

        SherryWeakSafeLock(Object key, Interner<SLock<Lock>> context) {
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
        public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
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

    class SherryWeakSafeReadWriteLock extends SLock<ReadWriteLock> implements ReadWriteLock {

        public SherryWeakSafeReadWriteLock(Object key, Interner<SLock<ReadWriteLock>> context) {
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
