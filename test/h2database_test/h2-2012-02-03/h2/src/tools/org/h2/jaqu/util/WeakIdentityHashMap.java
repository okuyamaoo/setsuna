/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jaqu.util;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This hash map uses weak references, so that elements that are no longer
 * referenced elsewhere can be garbage collected. It also uses object identity
 * to compare keys. The garbage collection happens when trying to add new data,
 * or when resizing.
 *
 * @param <K> the keys
 * @param <V> the value
 */
public class WeakIdentityHashMap<K, V> implements Map<K, V> {

    private static final int MAX_LOAD = 90;
    private static final WeakReference<Object> DELETED_KEY =
        new WeakReference<Object>(null);
    private int mask, len, size, deletedCount, level;
    private int maxSize, minSize, maxDeleted;
    private WeakReference<K>[] keys;
    private V[] values;

    public WeakIdentityHashMap() {
        reset(2);
    }

    public int size() {
        return size;
    }

    private void checkSizePut() {
        if (deletedCount > size) {
            rehash(level);
        }
        if (size + deletedCount >= maxSize) {
            rehash(level + 1);
        }
    }

    private void checkSizeRemove() {
        if (size < minSize && level > 0) {
            rehash(level - 1);
        } else if (deletedCount > maxDeleted) {
            rehash(level);
        }
    }

    private int getIndex(Object key) {
        return System.identityHashCode(key) & mask;
    }

    @SuppressWarnings("unchecked")
    private void reset(int newLevel) {
        minSize = size * 3 / 4;
        size = 0;
        level = newLevel;
        len = 2 << level;
        mask = len - 1;
        maxSize = (int) (len * MAX_LOAD / 100L);
        deletedCount = 0;
        maxDeleted = 20 + len / 2;
        keys = new WeakReference[len];
        values = (V[]) new Object[len];
    }

    public V put(K key, V value) {
        checkSizePut();
        int index = getIndex(key);
        int plus = 1;
        int deleted = -1;
        do {
            WeakReference<K> k = keys[index];
            if (k == null) {
                // found an empty record
                if (deleted >= 0) {
                    index = deleted;
                    deletedCount--;
                }
                size++;
                keys[index] = new WeakReference<K>(key);
                values[index] = value;
                return null;
            } else if (k == DELETED_KEY) {
                if (deleted < 0) {
                    // found the first deleted record
                    deleted = index;
                }
            } else {
                Object r = k.get();
                if (r == null) {
                    delete(index);
                } else if (r == key) {
                    // update existing
                    V old = values[index];
                    values[index] = value;
                    return old;
                }
            }
            index = (index + plus++) & mask;
        } while(plus <= len);
        throw new RuntimeException("Hashmap is full");
    }

    public V remove(Object key) {
        checkSizeRemove();
        int index = getIndex(key);
        int plus = 1;
        do {
            WeakReference<K> k = keys[index];
            if (k == null) {
                // found an empty record
                return null;
            } else if (k == DELETED_KEY) {
                // continue
            } else {
                Object r = k.get();
                if (r == null) {
                    delete(index);
                } else if (r == key) {
                    // found the record
                    V old = values[index];
                    delete(index);
                    return old;
                }
            }
            index = (index + plus++) & mask;
            k = keys[index];
        } while(plus <= len);
        // not found
        return null;
    }

    @SuppressWarnings("unchecked")
    private void delete(int index) {
        keys[index] = (WeakReference<K>) DELETED_KEY;
        values[index] = null;
        deletedCount++;
        size--;
    }

    private void rehash(int newLevel) {
        WeakReference<K>[] oldKeys = keys;
        V[] oldValues = values;
        reset(newLevel);
        for (int i = 0; i < oldKeys.length; i++) {
            WeakReference<K> k = oldKeys[i];
            if (k != null && k != DELETED_KEY) {
                K key = k.get();
                if (key != null) {
                    put(key, oldValues[i]);
                }
            }
        }
    }

    public V get(Object key) {
        int index = getIndex(key);
        int plus = 1;
        do {
            WeakReference<K> k = keys[index];
            if (k == null) {
                return null;
            } else if (k == DELETED_KEY) {
                // continue
            } else {
                Object r = k.get();
                if (r == null) {
                    delete(index);
                } else if (r == key) {
                    return values[index];
                }
            }
            index = (index + plus++) & mask;
        } while(plus <= len);
        return null;
    }

    public void clear() {
        reset(2);
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        for (V item: values) {
            if (value.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public Set<java.util.Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

}
