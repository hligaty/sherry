package io.github.hligaty.reference;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 生成可重新创建的对象。对于可重新创建的对象，解决自动删除其已被丢弃的WeakHashMap条目可能会令人困惑的问题
 *
 * @param <E> 维护的对象类型
 * @author hligaty
 * @date 2022/11/21
 * @see java.util.WeakHashMap
 */
public class Interner<E> {
    private final WeakHashMap<E, WeakReference<E>> cache = new WeakHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    public E intern(E sample) {
        cacheLock.readLock().lock();
        try {
            WeakReference<E> ref = cache.get(sample);
            if (ref != null) {
                return ref.get();
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        cacheLock.writeLock().lock();
        try {
            WeakReference<E> newRef = cache.get(sample);
            if (newRef == null) {
                newRef = new WeakReference<>(sample);
                cache.put(sample, newRef);
                return sample;
            }
            return newRef.get();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
