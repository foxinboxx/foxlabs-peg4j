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

import java.io.Reader;
import java.io.IOException;

import org.foxlabs.util.Location;

public class BacktrackingReader extends Reader {
    
    public static final int EOF = -1;
    public static final int EOL = '\n';
    
    private static final int MIN_BUFFER_DELTA_SIZE = 4096;
    private static final int EXPECTED_LINE_LENGTH = 80;
    private static final int INITIAL_MARKER_SIZE = 50;
    
    private final Reader in;
    private final String file;
    
    private char[] buffer = new char[MIN_BUFFER_DELTA_SIZE];
    private int size = 0;
    private int offset = 0;
    private int line = 0;
    private int column = 0;
    
    private int[] markOffsets = new int[INITIAL_MARKER_SIZE];
    private int[] markLines = new int[INITIAL_MARKER_SIZE];
    private int[] markColumns = new int[INITIAL_MARKER_SIZE];
    private int marker = 0;
    
    public BacktrackingReader(Reader in) {
        this(in, null, 1, 1);
    }
    
    public BacktrackingReader(Reader in, Location start) {
        this(in, start.file, start.line, start.column);
    }
    
    public BacktrackingReader(Reader in, String file) {
        this(in, file, 1, 1);
    }
    
    public BacktrackingReader(Reader in, int line, int column) {
        this(in, null, line, column);
    }
    
    public BacktrackingReader(Reader in, String file, int line, int column) {
        this.in = in;
        this.file = file;
        this.markLines[0] = this.line = line < 1 ? 1 : line;
        this.markColumns[0] = this.column = column < 1 ? 1 : column;
        this.markOffsets[0] = 0;
    }
    
    public String getFile() {
        return file;
    }
    
    public int getStartOffset() {
        return markOffsets[marker];
    }
    
    public int getEndOffset() {
        return offset;
    }
    
    public int getStartLine() {
        return markLines[marker];
    }
    
    public int getEndLine() {
        return line;
    }
    
    public int getStartColumn() {
        return markColumns[marker];
    }
    
    public int getEndColumn() {
        return column;
    }
    
    public Location getStart() {
        return Location.valueOf(file, markLines[marker], markColumns[marker]);
    }
    
    public Location getEnd() {
        return Location.valueOf(file, line, column);
    }
    
    public String getText() {
        int length = getLength();
        if (length == 0)
            return null;
        return new String(buffer, markOffsets[marker], length);
    }
    
    public char[] getChars() {
        int start = markOffsets[marker];
        int length = offset - start;
        char[] chars = new char[length];
        System.arraycopy(buffer, start, chars, 0, length);
        return chars;
    }
    
    public int getLength() {
        return offset - markOffsets[marker];
    }
    
    public int read() throws IOException {
        if (offset == size) {
            int count = fillBuffer(1);
            if (count < 0)
                return EOF;
        }
        return readBuffer();
    }
    
    public int read(char[] buffer, int offset, int length) throws IOException {
        if (length < 0 || offset < 0 || offset + length > buffer.length)
            throw new IndexOutOfBoundsException();
        if (length == 0)
            return 0;
        
        if (length > 0) {
            length = fillBuffer(length);
            if (length > 0) {
                for (int i = offset, j = offset + length; i < j; i++)
                    buffer[i] = readBuffer();
            }
        }
        
        return length;
    }
    
    public String readLine() throws IOException {
        int start = offset;
        int length = 0;
        
        int count = fillBuffer(EXPECTED_LINE_LENGTH);
        for (; count > 0; count = fillBuffer(EXPECTED_LINE_LENGTH))
            for (int i = 0; i < count; i++, length++)
                if (readBuffer() == '\n')
                    return new String(buffer, start, length);
        
        return length > 0 ? new String(buffer, start, length) : null;
    }
    
    public long skip(long count) throws IOException {
        if (count < 0L)
            throw new IllegalArgumentException();
        
        count = fillBuffer((int) count); // long is not supported
        for (int i = 0; i < count; i++)
            readBuffer(); // update location
        
        return count;
    }
    
    public int skip(int offset, int line, int column) throws IOException {
        if (offset < this.offset || offset > this.size)
            throw new IllegalStateException();
        if (line < this.line)
            throw new IllegalStateException();
        if (line == this.line)
            if (column < this.column)
                throw new IllegalStateException();
        
        int oldOffset = this.offset;
        
        this.offset = offset;
        this.line = line;
        this.column = column;
        
        return offset - oldOffset;
    }
    
    public int skipWhitespace() throws IOException {
        int length = 0;
        
        int count = fillBuffer(EXPECTED_LINE_LENGTH);
        for (; count > 0; count = fillBuffer(EXPECTED_LINE_LENGTH))
            for (int i = 0; i < count; i++, length++)
                if (Character.isWhitespace(buffer[offset])) {
                    readBuffer();
                } else {
                    return length;
                }
        
        return length;
    }
    
    @Override
    public boolean ready() throws IOException {
        return offset < size || in.ready();
    }
    
    @Override
    public boolean markSupported() {
        return true;
    }
    
    @Override
    public void mark(int unused) {
        mark();
    }
    
    /**
     * Stores a pointer to the current position in character stream.
     * 
     * @see #release()
     * @see #reset()
     * @see #consume()
     */
    public void mark() {
        marker++;
        
        if (marker == markOffsets.length) {
            int newLength = markOffsets.length * 2;
            markOffsets = Arrays.copyOf(markOffsets, newLength);
            markLines = Arrays.copyOf(markLines, newLength);
            markColumns = Arrays.copyOf(markColumns, newLength);
        }
        
        markOffsets[marker] = offset;
        markLines[marker] = line;
        markColumns[marker] = column;
    }
    
    /**
     * Releases a previously stored pointer by the {@link #mark()} method.
     * 
     * @throws IllegalStateException if there are no previously stored pointers.
     */
    public void release() {
        if (marker == 0)
            throw new IllegalStateException();
        
        marker--;
    }
    
    /**
     * Resets position in character stream to the previously stored by the
     * {@link #mark()} method and releases the pointer.
     * 
     * @throws IllegalStateException if there are no previously stored pointers.
     */
    public void reset() {
        if (marker == 0)
            throw new IllegalStateException();
        
        offset = markOffsets[marker];
        line = markLines[marker];
        column = markColumns[marker];
        
        marker--;
    }
    
    public void consume() {
        int length = getLength();
        if (length > 0) {
            int start = markOffsets[marker];
            System.arraycopy(buffer, offset, buffer, start, size -= length);
            offset = start;
        }
    }
    
    public void flush() {
        markOffsets[0] = 0;
        markLines[0] = line;
        markColumns[0] = column;
        marker = 0;
        
        if (offset > 0) {
            System.arraycopy(buffer, offset, buffer, 0, size -= offset);
            offset = 0;
        }
    }
    
    public void close() throws IOException {
        in.close();
    }
    
    private void ensureCapacity(int count) {
        if (size + count > buffer.length)
            buffer = Arrays.copyOf(buffer, buffer.length * 3 / 2 + count);
    }
    
    private char readBuffer() {
        char ch = buffer[offset++];
        if (ch == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return ch;
    }
    
    private int fillBuffer(int count) throws IOException {
        int rem = size - offset;
        if (rem >= count)
            return count;
        
        int delta = (count - rem) * 2; // to avoid long sequence of the \n\r
        if (delta < MIN_BUFFER_DELTA_SIZE)
            delta = MIN_BUFFER_DELTA_SIZE;
        
        ensureCapacity(delta);
        int length = in.read(buffer, size, delta);
        if (length < 0)
            return rem == 0 ? EOF : rem;
        
        for (int i = size, j = size + length; i < j;) {
            int ch = buffer[i++];
            switch (ch) {
                case '\n':
                    buffer[size++] = '\n';
                    if (i < j) {
                        if (buffer[i] == '\r') i++;
                    } else {
                        do { // read until \n? sequence ends
                            ch = in.read();
                            if (!(ch < 0 || ch == '\r')) {
                                ensureCapacity(1);
                                buffer[size++] = (char) ch;
                            }
                        } while (ch == '\n');
                    }
                    break;
                
                case '\r':
                    buffer[size++] = '\n';
                    if (i < j) {
                        if (buffer[i] == '\n') i++;
                    } else {
                        do { // read until \r? sequence ends
                            ch = in.read();
                            if (!(ch < 0 || ch == '\n')) {
                                ensureCapacity(1);
                                buffer[size++] = ch == '\r' ? '\n' : (char) ch;
                            }
                        } while (ch == '\r');
                    }
                    break;
                
                default:
                    buffer[size++] = (char) ch;
                    break;
            }
        }
        
        return size - offset;
    }
    
}
