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

/**
 * Stack of elements with backtracking capability.
 * 
 * <p> Method {@link #mark()} saves current position in the stack. Method
 * {@link #release()} releases previous mark meaning that marker is deleted.
 * Method {@link #reset()} resets current position in the stack to previously
 * marked position meaning that elements pushed after this mark are lost.
 * Depth of markers is not limited. </p>
 * 
 * @param <E> Type of elements maintained by this stack.
 * 
 * @author Fox Mulder
 */
public class BacktrackingStack<E> {
    
    /**
     * Stack of elements.
     */
    private Object[] elements;
    
    /**
     * Current number of elements in the stack.
     */
    private int size = 0;
    
    /**
     * Stack of saved markers.
     */
    private int[] marks = new int[32];
    
    /**
     * Current depth of markers stack.
     */
    private int marker = 0;
    
    /**
     * Constructs a new stack with initial capacity of 16 elements.
     */
    public BacktrackingStack() {
        this(16);
    }
    
    /**
     * Constructs a new stack with the specified initial capacity.
     * 
     * @param initialCapacity Initial capacity of this stack.
     */
    public BacktrackingStack(int initialCapacity) {
        this.elements = new Object[initialCapacity];
    }
    
    /**
     * Determines if this stack has no elements starting from previously saved
     * position by latest {@link #mark()} method call or this stack of elements
     * and markers stack are empty.
     * 
     * @return <code>true</code> if this stack has no elements;
     *         <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Returns number of elements in this stack starting from previously saved
     * position by latest {@link #mark()} method call or total number of
     * elements if markers stack is empty.
     * 
     * @return Number of elements in this stack.
     */
    public int size() {
        return marker > 0 ? size - marks[marker - 1] : size;
    }
    
    /**
     * Stores a pointer to the current position in this stack.
     * 
     * @return This stack instance.
     * @see #reset()
     * @see #release()
     */ 
    public BacktrackingStack<E> mark() {
        if (marker == marks.length) {
            int[] copy = new int[marks.length * 2];
            System.arraycopy(marks, 0, copy, 0, marks.length);
            marks = copy;
        }
        marks[marker++] = size;
        return this;
    }
    
    /**
     * Resets position in this stack to the previously stored by the
     * {@link #mark()} method and releases the pointer.
     * 
     * @return This stack instance.
     * @throws IllegalStateException if there are no previously stored pointers.
     * @see #mark()
     */
    public BacktrackingStack<E> reset() {
        if (marker == 0) {
            throw new IllegalStateException();
        } else {
            Arrays.fill(elements, marks[--marker], size, null);
            size = marks[marker];
            return this;
        }
    }
    
    /**
     * Releases a previously stored pointer by the {@link #mark()} method.
     * 
     * @return This stack instance.
     * @throws IllegalStateException if there are no previously stored pointers.
     * @see #mark()
     */
    public BacktrackingStack<E> release() {
        if (marker == 0) {
            throw new IllegalStateException();
        } else {
            marker--;
            return this;
        }
    }
    
    /**
     * Pushes the specified element onto this stack.
     * 
     * @param element Element to push.
     */
    public void push(E element) {
        ensureCapacity(1);
        elements[size++] = element;
    }
    
    /**
     * Pushes the specified array of elements onto this stack.
     * 
     * @param elements Array of elements to push.
     */
    @SuppressWarnings("unchecked")
    public void pushAll(E... elements) {
        ensureCapacity(elements.length);
        System.arraycopy(elements, 0, this.elements, size, elements.length);
        size += elements.length;
    }
    
    /**
     * Pops an element from this stack.
     * 
     * @return Element at the front of this stack.
     * @throws IllegalStateException if stack is empty or there are no elements
     *         pushed after last {@link #mark()} call.
     */
    public E pop() {
        if (isEmpty()) {
            throw new IllegalStateException();
        } else {
            E element = Types.cast(elements[--size]);
            elements[size] = null;
            return element;
        }
    }
    
    /**
     * Pops elements from this stack into the specified array starting from
     * previously saved position by latest {@link #mark()} call or from the
     * beginning of this stack if markers stack is empty. If number of elements
     * in the stack is greater than length of the specified array then array
     * will be filled with first front elements.
     * 
     * @return Array of elements at the front of this stack.
     */
    public E[] popAll(E[] array) {
        int length = Math.min(marker == 0 ? size : size - marks[marker - 1], array.length);
        System.arraycopy(elements, size - length, array, 0, length);
        Arrays.fill(elements, size - length, size, null);
        size -= length;
        return array;
    }
    
    /**
     * Retrieves, but does not remove, last element of this stack.
     * 
     * @return Element at the front of this stack.
     * @throws IllegalStateException if stack is empty or there are no elements
     *         pushed after last {@link #mark()} call.
     */
    public E peek() {
        if (isEmpty()) {
            throw new IllegalStateException();
        } else {
            return Types.cast(elements[size - 1]);
        }
    }
    
    /**
     * Retrieves, but does not remove, elements from this stack into the
     * specified array starting from previously saved position by latest
     * {@link #mark()} call or from the beginning of this stack if markers
     * stack is empty. If number of elements in the stack is greater than
     * length of the specified array then array will be filled with first front
     * elements.
     * 
     * @return Array of elements at the front of this stack.
     */
    public E[] peekAll(E[] array) {
        int length = Math.min(marker == 0 ? size : size - marks[marker - 1], array.length);
        System.arraycopy(elements, size - length, array, 0, length);
        return array;
    }
    
    /**
     * Removes all of the elements from this stack.
     */
    public void clear() {
        Arrays.fill(elements, 0, size, null);
        size = marker = 0;
    }
    
    /**
     * Returns string representation of this stack elements starting from
     * previously saved position by latest {@link #mark()} call or from the
     * beginning of this stack if markers stack is empty.
     * 
     * @return String representation of this stack elements.
     */
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
    
    /**
     * Increases capacity of this stack if necessary to ensure that it can hold
     * the specified number of additional elements.
     * 
     * @param delta Number of additional elements.
     */
    private void ensureCapacity(int delta) {
        if (size + delta > elements.length) {
            Object[] copy = new Object[elements.length * 3 / 2 + delta];
            System.arraycopy(elements, 0, copy, 0, size);
            elements = copy;
        }
    }
    
}
