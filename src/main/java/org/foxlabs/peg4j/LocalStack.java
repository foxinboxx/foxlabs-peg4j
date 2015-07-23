/* 
 * Copyright (C) 2014 FoxLabs
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

package org.foxlabs.peg4j;

import java.util.Arrays;

import org.foxlabs.util.reflect.Types;

public class LocalStack<E> {
    
    private Object[] elements;
    private int size = 0;
    
    private int[] marks = new int[32];
    private int marker = 0;
    
    public LocalStack() {
        this(16);
    }
    
    public LocalStack(int initialCapacity) {
        this.elements = new Object[initialCapacity];
    }
    
    public boolean isEmpty() {
        return size() == 0;
    }
    
    public int size() {
        return marker > 0 ? size - marks[marker - 1] : size;
    }
    
    public LocalStack<E> mark() {
        if (marker == marks.length) {
            int[] copy = new int[marks.length * 2];
            System.arraycopy(marks, 0, copy, 0, marks.length);
            marks = copy;
        }
        marks[marker++] = size;
        return this;
    }
    
    public LocalStack<E> reset() {
        if (marker == 0) {
            throw new IllegalStateException();
        } else {
            Arrays.fill(elements, marks[--marker], size, null);
            size = marks[marker];
            return this;
        }
    }
    
    public LocalStack<E> release() {
        if (marker == 0) {
            throw new IllegalStateException();
        } else {
            marker--;
            return this;
        }
    }
    
    public void push(E element) {
        ensureCapacity(1);
        elements[size++] = element;
    }
    
    public void pushAll(E... elements) {
        ensureCapacity(elements.length);
        System.arraycopy(elements, 0, this.elements, size, elements.length);
        size += elements.length;
    }
    
    public E pop() {
        if (isEmpty()) {
            throw new IllegalStateException();
        } else {
            E element = Types.cast(elements[--size]);
            elements[size] = null;
            return element;
        }
    }
    
    public E[] popAll(E[] array) {
        int length = Math.min(marker == 0 ? size : size - marks[marker - 1], array.length);
        System.arraycopy(elements, size - length, array, 0, length);
        Arrays.fill(elements, size - length, size, null);
        size -= length;
        return array;
    }
    
    public E peek() {
        if (isEmpty()) {
            throw new IllegalStateException();
        } else {
            return Types.cast(elements[size - 1]);
        }
    }
    
    public E[] peekAll(E[] array) {
        int length = Math.min(marker == 0 ? size : size - marks[marker - 1], array.length);
        System.arraycopy(elements, size - length, array, 0, length);
        return array;
    }
    
    public void clear() {
        Arrays.fill(elements, 0, size, null);
        size = marker = 0;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        int start = marker == 0 ? 0 : marks[marker - 1];
        if (start < size) {
            buf.append(elements[start]);
            for (int i = start + 1; i < size; i++) {
                buf.append(',');
                buf.append(elements[i]);
            }
        }
        buf.append(']');
        return buf.toString();
    }
    
    private void ensureCapacity(int delta) {
        if (size + delta > elements.length) {
            Object[] copy = new Object[elements.length * 3 / 2 + delta];
            System.arraycopy(elements, 0, copy, 0, size);
            elements = copy;
        }
    }
    
}
