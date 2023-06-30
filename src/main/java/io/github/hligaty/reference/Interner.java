package io.github.hligaty.reference;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 生成可重新创建的对象。对于可重新创建的对象，解决自动删除其已被丢弃的WeakHashMap条目可能会令人困惑的问题
 *
 * @param <K> 维护的对象类型
 * @author hligaty
 * @see java.util.WeakHashMap
 */
public class Interner<K> {
    private final WeakHashMap<K, WeakReference<K>> cache = new WeakHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    public K intern(K key) {
        cacheLock.readLock().lock();
        try {
            WeakReference<K> ref = cache.get(key);
            if (ref != null) {
                return ref.get();
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        cacheLock.writeLock().lock();
        try {
            WeakReference<K> newRef = cache.get(key);
            if (newRef == null) {
                newRef = new WeakReference<>(key);
                cache.put(key, newRef);
                return key;
            }
            return newRef.get();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
