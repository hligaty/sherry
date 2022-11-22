package io.github.hligaty.util;

import java.lang.ref.WeakReference;
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
