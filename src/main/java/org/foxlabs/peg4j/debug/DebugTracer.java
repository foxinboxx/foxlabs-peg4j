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

package org.foxlabs.peg4j.debug;

import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;

import org.foxlabs.peg4j.BacktrackingReader;
import org.foxlabs.peg4j.grammar.Reference;
import org.foxlabs.peg4j.grammar.Rule;
import org.foxlabs.peg4j.grammar.Action;
import org.foxlabs.peg4j.grammar.Terminal;
import org.foxlabs.peg4j.grammar.Production;
import org.foxlabs.util.Location;
import org.foxlabs.util.UnicodeSet;
import org.foxlabs.util.PeriodCounter;

public class DebugTracer implements RuleTracer {
    
    protected File file;
    protected Writer out;
    
    protected BacktrackingReader stream;
    
    protected TraceLevel traceLevel = TraceLevel.MEDIUM;
    protected int maxDepthLevel = 0;
    protected int maxTextSize = 80;
    protected int identSize = 2;
    
    protected long startTime;
    
    protected int ruleCount;
    protected int terminalCount;
    protected int productionCount;
    protected int memoCacheSize;
    protected int memoCacheHitCount;
    protected int memoCacheMissCount;
    protected int actionCount;
    protected int maxDepth;
    
    protected int depthLevel;
    protected int ident;
    
    public DebugTracer() {
        this(new OutputStreamWriter(System.out));
    }
    
    public DebugTracer(File file) {
        this.file = file;
    }
    
    public DebugTracer(Writer out) {
        this.out = out;
    }
    
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }
    
    public void setTraceLevel(TraceLevel level) {
        this.traceLevel = level;
    }
    
    public int getMaxDepthLevel() {
        return maxDepthLevel;
    }
    
    public void setMaxDepthLevel(int level) {
        this.maxDepthLevel = level < 0 ? 0 : level;
    }
    
    public int getMaxTextSize() {
        return maxTextSize;
    }
    
    public void setMaxTextSize(int size) {
        this.maxTextSize = size < 0 ? 0 : size;
    }
    
    public int getIdentSize() {
        return identSize;
    }
    
    public void setIdentSize(int size) {
        this.identSize = size < 0 ? 0 : size > 8 ? 8 : size;
    }
    
    @Override
    public void open(BacktrackingReader stream) throws IOException {
        if (file != null) {
            out = new FileWriter(file);
        }
        
        out.write(new java.util.Date().toString());
        out.write("\nSOURCE: ");
        out.write(stream.getStart().toString());
        
        out.write("\n\nOPTIONS:");
        out.write("\nTraceLevel: ");
        out.write(traceLevel.toString());
        out.write("\nMaxDepthLevel: ");
        out.write(Integer.toString(maxDepthLevel));
        out.write("\nMaxTextSize: ");
        out.write(Integer.toString(maxTextSize));
        
        out.write("\n\nTRACE:\n");
        
        this.stream = stream;
        
        startTime = System.currentTimeMillis();
        
        ruleCount = 0;
        terminalCount = 0;
        productionCount = 0;
        actionCount = 0;
        memoCacheSize = 0;
        memoCacheHitCount = 0;
        memoCacheMissCount = 0;
        maxDepth = 0;
        
        depthLevel = 0;
    }
    
    @Override
    public void trace(Rule rule) throws IOException {
        if (rule instanceof Terminal) {
            terminalCount++;
        } else if (rule instanceof Production) {
            productionCount++;
        } else if (rule instanceof Action) {
            actionCount++;
        }
        
        if (traceLevel.compareTo(TraceLevel.forRule(rule)) >= 0) {
            if (maxDepthLevel == 0 || depthLevel < maxDepthLevel) {
                out.write("\u0020\u0020");
                writeIdent();
                
                out.write("->\u0020");
                writeLocation(stream.getStart());
                out.write("\u0020");
                out.write(rule.toString());
                out.write("\n");
                
                ident++;
            }
        }
        
        ruleCount++;
        depthLevel++;
        if (depthLevel > maxDepth) {
            maxDepth = depthLevel;
        }
    }
    
    @Override
    public void backtrace(Rule rule, boolean success) throws IOException {
        depthLevel--;
        if (traceLevel.compareTo(TraceLevel.forRule(rule)) >= 0) {
            if (maxDepthLevel == 0 || depthLevel < maxDepthLevel) {
                ident--;
                
                out.write(success ? "\u0020\u0020" : "!\u0020");
                writeIdent();
                
                out.write("<-\u0020");
                writeLocation(stream.getStart());
                out.write("\u0020");
                out.write(rule.toString());
                out.write("\n");
                
                if (success && maxTextSize > 0) {
                    String text = stream.getText();
                    int length = text == null ? 0 : text.length();
                    if (length > 0) {
                        out.write("\u0020\u0020");
                        writeIdent();
                        out.write("\u0020\u0020\u0020");
                        
                        if (length > maxTextSize) {
                            out.write("\"...");
                            text = text.substring(text.length() - maxTextSize);
                        } else {
                            out.write("\"");
                        }
                        out.write(UnicodeSet.escape(text));
                        out.write("\"\n");
                    }
                }
            }
        }
    }
    
    @Override
    public void lookup(Reference reference, boolean hit) throws IOException {
        if (hit) {
            memoCacheHitCount++;
        } else {
            memoCacheMissCount++;
        }
    }
    
    @Override
    public void cache(Reference reference) throws IOException {
        memoCacheSize++;
    }
    
    @Override
    public void close(boolean result) throws IOException {
        out.write("\n\nTRACE RESULTS:");
        
        out.write("\nTracing time: ");
        out.write(PeriodCounter.encodeDuration(System.currentTimeMillis() - startTime));
        
        out.write("\nRule invocations: ");
        out.write(Integer.toString(ruleCount));
        out.write("\nTerminal invocations: ");
        out.write(Integer.toString(terminalCount));
        out.write("\nProduction invocations: ");
        out.write(Integer.toString(productionCount));
        out.write("\nAction invocations: ");
        out.write(Integer.toString(actionCount));
        out.write("\nMemo cache size: ");
        out.write(Integer.toString(memoCacheSize));
        out.write("\nMemo cache hits: ");
        out.write(Integer.toString(memoCacheHitCount));
        out.write("\nMemo cache misses: ");
        out.write(Integer.toString(memoCacheMissCount));
        out.write("\nMax depth: ");
        out.write(Integer.toString(maxDepth));
        
        out.write(result ? "\n\nSUCCESS!\n" : "\n\nFAILURE!\n");
        
        if (file != null) {
            out.close();
        }
    }
    
    protected void writeIdent() throws IOException {
        for (int i = ident * identSize; i > 0; i--) {
            out.write("\u0020");
        }
    }
    
    protected void writeLocation(Location location) throws IOException {
        out.write(Integer.toString(location.line));
        out.write(":");
        out.write(Integer.toString(location.column));
    }
    
}
