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

package org.foxlabs.peg4j.grammar;

import java.util.Arrays;

import org.foxlabs.util.Location;

public final class Grammar {
    
    private final Production[] productions;
    private final GrammarProblems problems;
    private final String source;
    
    private int[] offsets;
    
    Grammar(Production[] productions, GrammarProblems problems, String source) {
        this.productions = productions;
        this.problems = problems;
        this.source = source;
    }
    
    public Production getStart() {
        return productions.length == 0 ? null : productions[0];
    }
    
    public int getProductionCount() {
        return productions.length;
    }
    
    public Production getProduction(int index) {
        return productions[index];
    }
    
    public Production[] getProductions() {
        return productions.clone();
    }
    
    public boolean hasProblems() {
        return !problems.isEmpty();
    }
    
    public boolean hasErrors() {
        return problems.hasFatals() || problems.hasErrors();
    }
    
    public GrammarProblems getProblems() {
        return problems;
    }
    
    public String getFile() {
        Production start = getStart();
        return start == null || start.start == null ? null : start.getStart().file;
    }
    
    public boolean hasSource() {
        if (source == null)
            return false;
        
        if (offsets == null) {
            int count = 0;
            offsets = new int[256];
            offsets[count++] = 0;
            int length = source.length();
            for (int i = 0; i < length; i++) {
                if (source.charAt(i) == '\n') {
                    if (count == offsets.length)
                        offsets = Arrays.copyOf(offsets, offsets.length * 2);
                    offsets[count++] = i + 1;
                }
            }
            offsets = Arrays.copyOf(offsets, count);
        }
        
        return true;
    }
    
    public int getLineCount() {
        return hasSource() ? offsets.length : 0;
    }
    
    public int getLineSize(int line) {
        checkSource();
        
        if (line < 1 || line > offsets.length)
            throw new IndexOutOfBoundsException();
        
        return offsets[line] - offsets[line - 1];
    }
    
    public String getSource() {
        return source;
    }
    
    public String getSource(int line) {
        checkSource();
        
        if (line < 1 || line > offsets.length)
            throw new IndexOutOfBoundsException();
        
        return source.substring(offsets[line - 1], offsets[line]);
    }
    
    public String getSource(int startLine, int startColumn, int endLine, int endColumn) {
        checkSource();
        
        int startLineSize = getLineSize(startLine);
        if (startColumn < 1 || startColumn > startLineSize)
            throw new IndexOutOfBoundsException();
        
        int endLineSize = getLineSize(startLine);
        if (endColumn < 1 || endColumn > endLineSize)
            throw new IndexOutOfBoundsException();
        
        int startIndex = offsets[startLine - 1] + startColumn - 1;
        int endIndex = offsets[endLine - 1] + endColumn - 1;
        return source.substring(startIndex, endIndex);
    }
    
    public String getSource(Location start, Location end) {
        if (start == Location.UNKNOWN || end == Location.UNKNOWN)
            throw new IllegalArgumentException();
        
        return getSource(start.line, start.column, end.line, end.column);
    }
    
    private void checkSource() {
        if (!hasSource())
            throw new IllegalStateException();
    }
    
    public String toString() {
        return toString(false);
    }
    
    public String toString(boolean debug) {
        StringBuilder buf = new StringBuilder();
        
        for (Production rule : productions)
            if (!(rule.getExpression() instanceof Terminal.Nil)) {
                rule.toString(buf, debug);
                buf.append(";\n");
            }
        
        if (hasProblems()) {
            buf.append("\n\n");
            problems.toString(buf);
        }
        
        return buf.toString();
    }
    
}
