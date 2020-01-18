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

/**
 * Class for reading character streams with backtracking capability. It keeps
 * all characters read in internal buffer.
 * 
 * <p> Method {@link #mark()} saves current position in character stream.
 * Method {@link #release()} releases previous mark meaning that marker is
 * deleted. Method {@link #reset()} resets current position in character stream
 * to previously marked position allowing to start reading from saved point.
 * Depth of markers is not limited. </p>
 * 
 * <p> Note that this reader implementation is not thread-safe. Also this
 * reader skips <code>\r</code> characters. </p>
 * 
 * @author Fox Mulder
 * @see Parser
 */
public class BacktrackingReader extends Reader {
    
    /**
     * End of stream.
     */
    public static final int EOF = -1;
    
    /**
     * End of line.
     */
    public static final int EOL = '\n';
    
    /**
     * Minimum delta to increase size of character buffer.
     */
    private static final int MIN_BUFFER_DELTA_SIZE = 4096;
    
    /**
     * Initial size of markers stack.
     */
    private static final int INITIAL_MARKER_SIZE = 50;
    
    /**
     * Underlying character stream.
     */
    private Reader in;
    
    /**
     * Name of the file (if any) associated with this character stream.
     */
    private final String file;
    
    /**
     * Character buffer.
     */
    private char[] buffer = new char[MIN_BUFFER_DELTA_SIZE];
    
    /**
     * Current number of characters in buffer.
     */
    private int size = 0;
    
    /**
     * Current offset in character buffer.
     */
    private int offset = 0;
    
    /**
     * Number of current line in character buffer (starts with 1).
     */
    private int line = 0;
    
    /**
     * Number of current column in character buffer (starts with 1).
     */
    private int column = 0;
    
    /**
     * Stack of saved marker offsets.
     */
    private int[] markOffsets = new int[INITIAL_MARKER_SIZE];
    
    /**
     * Stack of saved marker lines.
     */
    private int[] markLines = new int[INITIAL_MARKER_SIZE];
    
    /**
     * Stack of saved marker columns.
     */
    private int[] markColumns = new int[INITIAL_MARKER_SIZE];
    
    /**
     * Current number of saved markers in character buffer.
     */
    private int marker = 0;
    
    /**
     * Constructs a new backtracking reader with the specified underlying
     * character stream.
     * 
     * @param in Underlying character stream.
     */
    public BacktrackingReader(Reader in) {
        this(in, null, 1, 1);
    }
    
    /**
     * Constructs a new backtracking reader with the specified underlying
     * character stream and start location.
     * 
     * @param in Underlying character stream.
     * @param start Start location.
     */
    public BacktrackingReader(Reader in, Location start) {
        this(in, start.file, start.line, start.column);
    }
    
    /**
     * Constructs a new backtracking reader with the specified underlying
     * character stream and file associated with it.
     * 
     * @param in Underlying character stream.
     * @param file Name of the file associated with this character stream.
     */
    public BacktrackingReader(Reader in, String file) {
        this(in, file, 1, 1);
    }
    
    /**
     * Constructs a new backtracking reader with the specified underlying
     * character stream and start location.
     * 
     * @param in Underlying character stream.
     * @param line Start line.
     * @param column Start column.
     */
    public BacktrackingReader(Reader in, int line, int column) {
        this(in, null, line, column);
    }
    
    /**
     * Constructs a new backtracking reader with the specified underlying
     * character stream, file associated with it and start location.
     * 
     * @param in Underlying character stream.
     * @param file Name of the file associated with this character stream.
     * @param line Start line.
     * @param column Start column.
     */
    public BacktrackingReader(Reader in, String file, int line, int column) {
        this.in = in;
        this.file = file;
        this.markLines[0] = this.line = line < 1 ? 1 : line;
        this.markColumns[0] = this.column = column < 1 ? 1 : column;
        this.markOffsets[0] = 0;
    }
    
    /**
     * Returns name of the file associated with this character stream.
     * 
     * @return Name of the file associated with this character stream or
     *         <code>null</code> if no file associated.
     */
    public String getFile() {
        return file;
    }
    
    /**
     * Returns offset of previously saved postition in character stream by
     * latest {@link #mark()} method call.
     * 
     * @return Offset of previously saved postition in character stream by
     *         latest {@link #mark()} method call or 0 if marker stack is empty.
     * @see #mark()
     */
    public int getStartOffset() {
        return markOffsets[marker];
    }
    
    /**
     * Returns current offset in character stream.
     * 
     * @return Current offset in character stream.
     */
    public int getEndOffset() {
        return offset;
    }
    
    /**
     * Returns line number of previously saved postition in character stream by
     * latest {@link #mark()} method call.
     * 
     * @return Line number of previously saved postition in character stream by
     *         latest {@link #mark()} method call or 1 if marker stack is empty.
     * @see #mark()
     */
    public int getStartLine() {
        return markLines[marker];
    }
    
    /**
     * Returns current line number in character stream.
     * 
     * @return Current line number in character stream.
     */
    public int getEndLine() {
        return line;
    }
    
    /**
     * Returns column number of previously saved postition in character stream
     * by latest {@link #mark()} method call.
     * 
     * @return Column number of previously saved postition in character stream
     *         by latest {@link #mark()} method call or 1 if marker stack is
     *         empty.
     * @see #mark()
     */
    public int getStartColumn() {
        return markColumns[marker];
    }
    
    /**
     * Returns current column number in character stream.
     * 
     * @return Current column number in character stream.
     */
    public int getEndColumn() {
        return column;
    }
    
    /**
     * Returns location of previously saved postition in character stream by
     * latest {@link #mark()} method call.
     * 
     * @return Location of previously saved postition in character stream by
     *         latest {@link #mark()} method call.
     * @see #mark()
     * @see Location
     */
    public Location getStart() {
        return Location.valueOf(file, markLines[marker], markColumns[marker]);
    }
    
    /**
     * Returns current location in character stream.
     * 
     * @return Current location in character stream.
     * @see Location
     */
    public Location getEnd() {
        return Location.valueOf(file, line, column);
    }
    
    /**
     * Returns characters in character buffer as a string starting from
     * previously saved position by latest {@link #mark()} method call.
     * 
     * @return Characters in character buffer as a string starting from
     *         previously saved position by latest {@link #mark()} method call
     *         or all available characters if markers stack is empty.
     * @see #mark()
     */
    public String getText() {
        int length = getLength();
        if (length == 0) {
            return null;
        } else {
            return new String(buffer, markOffsets[marker], length);
        }
    }
    
    /**
     * Returns characters in character buffer as an array starting from
     * previously saved position by latest {@link #mark()} method call.
     * 
     * @return Characters in character buffer as an array starting from
     *         previously saved position by latest {@link #mark()} method call
     *         or all available characters if markers stack is empty.
     * @see #mark()
     */
    public char[] getChars() {
        int start = markOffsets[marker];
        int length = offset - start;
        char[] chars = new char[length];
        System.arraycopy(buffer, start, chars, 0, length);
        return chars;
    }
    
    /**
     * Returns number of characters in character buffer starting from previosly
     * saved position by latest {@link #mark()} method call.
     * 
     * @return Number of characters in character buffer starting from previosly
     *         saved position by latest {@link #mark()} method call or total
     *         number of characters if markers stack is empty.
     * @see #mark()
     */
    public int getLength() {
        return offset - markOffsets[marker];
    }
    
    /**
     * Reads a single character.
     * 
     * @return The character read as an integer or {@link #EOF} if the end of
     *         the stream has been reached.
     * @throws IOException if an IO error occurred.
     */
    public int read() throws IOException {
        ensureOpen();
        if (offset == size) {
            int count = fillBuffer(1);
            if (count < 0) {
                return EOF;
            }
        }
        return readBuffer();
    }
    
    /**
     * Reads characters into a portion of an array.
     * 
     * @param buffer Destination buffer.
     * @param offset Offset at which to start storing characters.
     * @param length Maximum number of characters to read.
     * @return The number of characters read or {@link #EOF} if the end of the
     *         stream has been reached.
     * @throws IOException if an IO error occurred.
     * @throws IndexOutOfBoundsException if the specified offset or length is
     *         negative or size of the specified buffer is not enough.
     */
    public int read(char[] buffer, int offset, int length) throws IOException {
        ensureOpen();
        if (length < 0 || offset < 0 || offset + length > buffer.length) {
            throw new IndexOutOfBoundsException();
        } else if (length == 0) {
            return 0;
        }
        
        if (length > 0) {
            length = fillBuffer(length);
            if (length > 0) {
                for (int i = offset, j = offset + length; i < j; i++) {
                    buffer[i] = readBuffer();
                }
            }
        }
        
        return length;
    }
    
    /**
     * Skips the specified number of characters.
     * 
     * <p> Note that maximum number of characters to skip is limited to
     * {@link Integer#MAX_VALUE}. </p>
     * 
     * @param count The number of characters to skip.
     * @return The number of characters actually skipped.
     * @throws IOException if an IO error occurred.
     * @throws IllegalArgumentException if the specifed number of characters to
     *         skip is negative or greater than {@link Integer#MAX_VALUE}.
     */
    public long skip(long count) throws IOException {
        ensureOpen();
        if (count < 0L || count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        
        count = fillBuffer((int) count); // long is not supported
        for (int i = 0; i < count; i++) {
            readBuffer(); // update location
        }
        
        return count;
    }
    
    /**
     * Tells whether this stream is ready to be read.
     * 
     * @return <code>true</code> if the next read is guaranteed not to block
     *         for input; <code>false</code> otherwise.
     * @throws IOException if an IO error occurred.
     */
    @Override
    public boolean ready() throws IOException {
        ensureOpen();
        return offset < size || in.ready();
    }
    
    /**
     * Returns <code>true</code>.
     * 
     * @return <code>true</code>.
     */
    @Override
    public boolean markSupported() {
        return true;
    }
    
    /**
     * Stores a pointer to the current position in character stream.
     * 
     * @param unused Argument is not used.
     * @throws IOException if stream has been closed.
     * @see #mark()
     */
    @Override
    public void mark(int unused) throws IOException {
        mark();
    }
    
    /**
     * Stores a pointer to the current position in character stream.
     * 
     * @throws IOException if stream has been closed.
     * @see #release()
     * @see #reset()
     */
    public void mark() throws IOException {
        ensureOpen();
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
     * @throws IOException if stream has been closed.
     * @throws IllegalStateException if there are no previously stored pointers.
     * @see #mark()
     */
    public void release() throws IOException {
        ensureOpen();
        if (marker == 0) {
            throw new IllegalStateException();
        } else {
            marker--;
        }
    }
    
    /**
     * Resets position in character stream to the previously stored by the
     * {@link #mark()} method and releases the pointer.
     * 
     * @throws IOException if stream has been closed.
     * @throws IllegalStateException if there are no previously stored pointers.
     * @see #mark()
     */
    public void reset() throws IOException {
        ensureOpen();
        if (marker == 0) {
            throw new IllegalStateException();
        } else {
            offset = markOffsets[marker];
            line = markLines[marker];
            column = markColumns[marker];
            marker--;
        }
    }
    
    /**
     * Closes the stream and releases any system resources associated with it.
     * 
     * @throws IOException if an IO error occurred.
     */
    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
    }
    
    /**
     * Checks to make sure that the stream has not been closed.
     * 
     * @throws IOException if stream has been closed.
     */
    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream has been closed");
        }
    }
    
    /**
     * Increases capacity of internal character buffer if necessary to ensure
     * that it can hold the specified number of additional characters.
     * 
     * @param count Number of additional characters.
     */
    private void ensureCapacity(int count) {
        if (size + count > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length * 3 / 2 + count);
        }
    }
    
    /**
     * Returns next character from internal character buffer and increases
     * current offset, line and column if necessary.
     * 
     * @return Next character from internal character buffer.
     */
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
    
    /**
     * Fills internal character buffer with the specified number of characters
     * from underlying character stream if necessary.
     * 
     * @param count Desired number of characters in buffer.
     * @return Actual number of characters in buffer available for read.
     * @throws IOException if an IO error occurred.
     */
    private int fillBuffer(int count) throws IOException {
        int rem = size - offset;
        if (rem >= count) {
            return count;
        }
        
        int delta = (count - rem) * 2; // to avoid long sequence of the \n\r
        if (delta < MIN_BUFFER_DELTA_SIZE) {
            delta = MIN_BUFFER_DELTA_SIZE;
        }
        
        ensureCapacity(delta);
        int length = in.read(buffer, size, delta);
        if (length < 0) {
            return rem == 0 ? EOF : rem;
        }
        
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
