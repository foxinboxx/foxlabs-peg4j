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

package org.foxlabs.peg4j.util;

import java.util.Arrays;

import org.foxlabs.util.reflect.Types;

public class LocalStack<E> {
    
    private static final int DEFAULT_CAPACITY = 20;
    
    protected E[] queue;
    protected int size = 0;
    
    protected int[] marks = new int[50];
    protected int marker = 0;
    
    protected LocalStack() {
        this(DEFAULT_CAPACITY);
    }
    
    protected LocalStack(int initialCapacity) {
        this.queue = Types.newArray(Types.parameterTypeOf(getClass(), LocalStack.class, 0),
                initialCapacity);
    }
    
    public LocalStack(Class<E> elementType) {
        this(elementType, DEFAULT_CAPACITY);
    }
    
    public LocalStack(Class<E> elementType, int initialCapacity) {
        this.queue = Types.newArray(elementType, initialCapacity);
    }
    
    public boolean isEmpty() {
        return size() == 0;
    }
    
    public int size() {
        return marker > 0 ? size - marks[marker - 1] : size;
    }
    
    public LocalStack<E> mark() {
        if (marker == marks.length)
            marks = Arrays.copyOf(marks, marks.length * 2);
        
        marks[marker++] = size;
        return this;
    }
    
    public LocalStack<E> reset() {
        if (marker == 0)
            throw new IllegalStateException();
        
        for (int i = marks[--marker]; i < size; i++)
            queue[i] = null;
        
        size = marks[marker];
        return this;
    }
    
    public LocalStack<E> release() {
        if (marker == 0)
            throw new IllegalStateException();
        
        marker--;
        return this;
    }
    
    private void ensureCapacity(int count) {
        if (size + count > queue.length)
            queue = Arrays.copyOf(queue, queue.length * 3 / 2 + count);
    }
    
    public E get(int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException();
        
        if (marker > 0) {
            int offset = marks[marker - 1] + index;
            if (offset >= size)
                throw new IndexOutOfBoundsException();
            return queue[offset];
        }
        
        if (index >= size)
            throw new IndexOutOfBoundsException();
        
        return queue[index];
    }
    
    public E[] getAll() {
        int endIndex = size;
        int beginIndex = (marker > 0 ? marks[marker - 1] : 0);
        
        return Arrays.copyOfRange(queue, beginIndex, endIndex);
    }
    
    public E getFirst() {
        return get(0);
    }
    
    public E getLast() {
        if (size > 0)
            if (marker > 0)
                if (marks[marker - 1] == size)
                    throw new IndexOutOfBoundsException();
        
        return queue[size - 1];
    }
    
    public void push(E element) {
        ensureCapacity(1);
        queue[size++] = element;
    }
    
    public void pushAll(E... elements) {
        ensureCapacity(elements.length);
        for (int i = 0; i < elements.length; i++)
            queue[size++] = elements[i];
    }
    
    public E pop() {
        if (size == 0 || marker > 0 && marks[marker - 1] == size)
            throw new IllegalStateException();
        
        E element = queue[--size];
        queue[size] = null;
        
        return element;
    }
    
    public E[] popAll() {
        int endIndex = size;
        int beginIndex = size = (marker > 0 ? marks[marker - 1] : 0);
        
        E[] elements = Types.newArray(queue.getClass().getComponentType(), endIndex - beginIndex);
        for (int i = beginIndex, j = 0; i < endIndex; i++, j++) {
            elements[j] = queue[i];
            queue[i] = null;
        }
        
        return elements;
    }
    
    public void clear() {
        for (int i = 0; i < size; i++)
            queue[i] = null;
        size = 0;
        marker = 0;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        int start = marker == 0 ? 0 : marks[marker - 1];
        if (start < size) {
            buf.append(queue[start]);
            for (int i = start + 1; i < size; i++) {
                buf.append(',');
                buf.append(queue[i]);
            }
        }
        buf.append(']');
        return buf.toString();
    }
    
}
