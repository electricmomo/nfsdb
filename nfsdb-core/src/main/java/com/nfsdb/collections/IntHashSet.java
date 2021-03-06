/*
 *  _  _ ___ ___     _ _
 * | \| | __/ __| __| | |__
 * | .` | _|\__ \/ _` | '_ \
 * |_|\_|_| |___/\__,_|_.__/
 *
 * Copyright (c) 2014-2015. The NFSdb project and its contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.collections;

import com.nfsdb.io.sink.CharSink;
import com.nfsdb.io.sink.StringSink;
import com.nfsdb.utils.Numbers;
import com.nfsdb.utils.Unsafe;

import java.util.Arrays;


public class IntHashSet implements Mutable {

    private static final int MIN_INITIAL_CAPACITY = 16;
    private static final int noEntryValue = -1;
    private final double loadFactor;
    private final IntList list;
    private int[] keys;
    private int free;
    private int capacity;
    private int mask;

    public IntHashSet() {
        this(8);
    }

    public IntHashSet(int initialCapacity) {
        this(initialCapacity, 0.5f);
    }

    @SuppressWarnings("unchecked")
    private IntHashSet(int initialCapacity, double loadFactor) {
        if (loadFactor <= 0d || loadFactor >= 1d) {
            throw new IllegalArgumentException("0 < loadFactor < 1");
        }
        this.list = new IntList(initialCapacity);
        int capacity = Math.max(initialCapacity, (int) (initialCapacity / loadFactor));
        this.loadFactor = loadFactor;
        keys = new int[capacity < MIN_INITIAL_CAPACITY ? MIN_INITIAL_CAPACITY : Numbers.ceilPow2(capacity)];
        mask = keys.length - 1;
        free = this.capacity = initialCapacity;
        clear();
    }

    public boolean add(int key) {
        boolean r = insertKey(key);
        if (r) {
            list.add(key);
            if (free == 0) {
                rehash();
            }
        }
        return r;
    }

    public final void clear() {
        free = capacity;
        Arrays.fill(keys, noEntryValue);
        list.clear();
    }

    public boolean contains(int key) {
        int index = key & mask;
        return Unsafe.arrayGet(keys, index) != noEntryValue && (key == Unsafe.arrayGet(keys, index) || key == Unsafe.arrayGet(keys, index)) || probeContains(key, index);
    }

    public int get(int index) {
        return list.getQuick(index);
    }

    public void remove(int key) {
        if (list.remove(key)) {
            int index = key & mask;
            if (key == Unsafe.arrayGet(keys, index)) {
                Unsafe.arrayPut(keys, index, noEntryValue);
                free++;
            } else {
                probeRemove(key, index);
            }
        }
    }

    public int size() {
        return capacity - free;
    }

    @Override
    public String toString() {
        StringSink sink = new StringSink();
        toString(sink);
        return sink.toString();
    }

    private boolean insertKey(int key) {
        int index = key & mask;
        if (Unsafe.arrayGet(keys, index) == noEntryValue) {
            Unsafe.arrayPut(keys, index, key);
            free--;
            return true;
        }
        return Unsafe.arrayGet(keys, index) != key && probeInsert(key, index);
    }

    private boolean probeContains(int key, int index) {
        do {
            index = (index + 1) & mask;
            if (Unsafe.arrayGet(keys, index) == noEntryValue) {
                return false;
            }

            if (key == Unsafe.arrayGet(keys, index)) {
                return true;
            }
        } while (true);
    }

    private boolean probeInsert(int key, int index) {
        do {
            index = (index + 1) & mask;
            if (Unsafe.arrayGet(keys, index) == noEntryValue) {
                Unsafe.arrayPut(keys, index, key);
                free--;
                return true;
            }

            if (key == Unsafe.arrayGet(keys, index)) {
                return false;
            }
        } while (true);
    }

    private void probeRemove(int key, int index) {
        int i = index;
        do {
            index = (index + 1) & mask;
            if (key == Unsafe.arrayGet(keys, index)) {
                Unsafe.arrayPut(keys, index, noEntryValue);
                free++;
                break;
            }
        } while (i != index);
    }

    @SuppressWarnings({"unchecked"})
    private void rehash() {
        int newCapacity = keys.length << 1;
        mask = newCapacity - 1;
        free = capacity = (int) (newCapacity * loadFactor);

        int[] oldKeys = keys;
        this.keys = new int[newCapacity];
        Arrays.fill(keys, noEntryValue);

        for (int i = oldKeys.length; i-- > 0; ) {
            if (Unsafe.arrayGet(oldKeys, i) != noEntryValue) {
                insertKey(Unsafe.arrayGet(oldKeys, i));
            }
        }
    }

    private void toString(CharSink sink) {
        sink.put('[');
        boolean needComma = false;
        for (int i = 0, n = keys.length; i < n; i++) {
            if (keys[i] != noEntryValue) {
                if (needComma) {
                    sink.put(',');
                }
                sink.put(keys[i]);

                if (!needComma) {
                    needComma = true;
                }
            }
        }

        sink.put(']');
    }
}
