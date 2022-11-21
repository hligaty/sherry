package io.github.hligaty.util;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Recreatable key objects.
 * With recreatable key objects,
 * the automatic removal of WeakHashMap entries whose keys have been discarded may prove to be confusing,
 * but WeakKey will not.
 *
 * @param <K> the type of keys maintained
 * @author hligaty
 * @see java.util.WeakHashMap
 */
public class WeakKey<K> {
    private static final WeakHashMap<WeakKey<?>, WeakReference<WeakKey<?>>> cache = new WeakHashMap<>();
    private static final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static final WeakHashMap<Thread, WeakKey<?>> shadowCache = new WeakHashMap<>();
    private static final ReadWriteLock shadowCacheLock = new ReentrantReadWriteLock();

    private K key;

    private WeakKey() {
    }

    @SuppressWarnings("unchecked")
    public static <T> WeakKey<T> wrap(T key) {
        WeakKey<T> shadow = (WeakKey<T>) getShadow();
        shadow.key = key;
        cacheLock.readLock().lock();
        try {
            WeakReference<WeakKey<?>> ref = cache.get(shadow);
            if (ref != null) {
                shadow.key = null;
                return (WeakKey<T>) ref.get();
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        cacheLock.writeLock().lock();
        try {
            WeakReference<WeakKey<?>> newRef = cache.get(shadow);
            shadow.key = null;
            if (newRef == null) {
                WeakKey<T> weakKey = new WeakKey<>();
                weakKey.key = key;
                newRef = new WeakReference<>(weakKey);
                cache.put(weakKey, newRef);
                return weakKey;
            }
            return (WeakKey<T>) newRef.get();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private static WeakKey<?> getShadow() {
        Thread thread = Thread.currentThread();
        shadowCacheLock.readLock().lock();
        WeakKey<?> shadow;
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
                shadow = new WeakKey<>();
                shadowCache.put(thread, shadow);
                return shadow;
            }
            return shadow;
        } finally {
            shadowCacheLock.writeLock().unlock();
        }
    }

    public K unwrap() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeakKey<?> weakKey = (WeakKey<?>) o;
        return Objects.equals(key, weakKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "WeakKey{" +
                "attr=" + key +
                '}';
    }
}
