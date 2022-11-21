package io.github.hligaty.util;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Generate recreatable key objects.
 * With recreatable key objects,
 * the automatic removal of WeakHashMap entries whose keys have been discarded may prove to be confusing,
 * but WeakKey will not.
 *
 * @param <K> the type of keys maintained
 * @author hligaty
 * @see java.util.WeakHashMap
 */
public class WeakSafeContext<K> {
    private final WeakHashMap<WeakObject<K>, WeakReference<WeakObject<K>>> cache = new WeakHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final WeakHashMap<Thread, WeakObject<K>> shadowCache = new WeakHashMap<>();
    private final ReadWriteLock shadowCacheLock = new ReentrantReadWriteLock();
    
    public WeakObject<K> wrap(K key) {
        WeakObject<K> shadow = getShadow();
        shadow.key = key;
        cacheLock.readLock().lock();
        try {
            WeakReference<WeakObject<K>> ref = cache.get(shadow);
            if (ref != null) {
                shadow.key = null;
                return ref.get();
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        cacheLock.writeLock().lock();
        try {
            WeakReference<WeakObject<K>> newRef = cache.get(shadow);
            shadow.key = null;
            if (newRef == null) {
                WeakObject<K> weakObject = new WeakObject<>();
                weakObject.key = key;
                newRef = new WeakReference<>(weakObject);
                cache.put(weakObject, newRef);
                return weakObject;
            }
            return newRef.get();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private WeakObject<K> getShadow() {
        Thread thread = Thread.currentThread();
        shadowCacheLock.readLock().lock();
        WeakObject<K> shadow;
        try {
            shadow = shadowCache.get(thread);
            if (shadow != null) {
                return shadow;
            }
        } finally {
            shadowCacheLock.readLock().unlock();
        }
        shadowCacheLock.writeLock().lock();
        try {
            shadow = shadowCache.get(thread);
            if (shadow == null) {
                shadow = new WeakObject<>();
                shadowCache.put(thread, shadow);
                return shadow;
            }
            return shadow;
        } finally {
            shadowCacheLock.writeLock().unlock();
        }
    }
    
    public static class WeakObject<K> {
        private K key;

        public K unwrap() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WeakObject<?> weakObject = (WeakObject<?>) o;
            return Objects.equals(key, weakObject.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
