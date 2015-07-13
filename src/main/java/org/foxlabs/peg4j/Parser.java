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

import java.net.URL;

import java.util.LinkedList;
import java.util.HashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.IOException;

import org.foxlabs.peg4j.grammar.Rule;
import org.foxlabs.peg4j.grammar.Action;
import org.foxlabs.peg4j.grammar.Production;
import org.foxlabs.peg4j.grammar.Grammar;
import org.foxlabs.peg4j.grammar.ParseContext;
import org.foxlabs.peg4j.debug.RuleTracer;
import org.foxlabs.peg4j.debug.ErrorTracer;

import org.foxlabs.util.Location;
import org.foxlabs.util.reflect.Types;

public abstract class Parser<T> {
    
    private RuleTracer tracer = null;
    private boolean memoable = true;
    
    // Interface
    
    protected abstract Grammar getGrammar();
    
    protected abstract Transaction startTransaction();
    
    protected abstract T buildResult();
    
    // Configuration
    
    public final RuleTracer getTracer() {
        return tracer;
    }
    
    public final void setTracer(RuleTracer tracer) {
        this.tracer = tracer;
    }
    
    public final boolean isMemoable() {
        return memoable;
    }
    
    public final void setMemoable(boolean memoable) {
        this.memoable = memoable;
    }
    
    // Parsing
    
    public final T parse(String text) throws IOException, RecognitionException {
        return parse(new BacktrackingReader(new StringReader(text)));
    }
    
    public final T parse(URL url) throws IOException, RecognitionException {
        InputStream stream = url.openStream();
        try {
            return parse(new BacktrackingReader(new InputStreamReader(stream), url.toString()));
        } finally {
            stream.close();
        }
    }
    
    public final T parse(URL url, String encoding) throws IOException, RecognitionException {
        InputStream stream = url.openStream();
        try {
            return parse(new BacktrackingReader(new InputStreamReader(stream, encoding), url.toString()));
        } finally {
            stream.close();
        }
    }
    
    public final T parse(File file) throws IOException, RecognitionException {
        FileInputStream stream = new FileInputStream(file);
        try {
            return parse(new BacktrackingReader(new InputStreamReader(stream), file.getPath()));
        } finally {
            stream.close();
        }
    }
    
    public final T parse(File file, String encoding) throws IOException, RecognitionException {
        FileInputStream stream = new FileInputStream(file);
        try {
            return parse(new BacktrackingReader(new InputStreamReader(stream, encoding), file.getPath()));
        } finally {
            stream.close();
        }
    }
    
    public final T parse(InputStream stream) throws IOException, RecognitionException {
        return parse(new BacktrackingReader(new InputStreamReader(stream)));
    }
    
    public final T parse(InputStream stream, String encoding) throws IOException, RecognitionException {
        return parse(new BacktrackingReader(new InputStreamReader(stream, encoding)));
    }
    
    public final T parse(Reader stream) throws IOException, RecognitionException {
        if (stream instanceof BacktrackingReader) {
            return parse((BacktrackingReader) stream);
        } else {
            return parse(new BacktrackingReader(stream));
        }
    }
    
    public final T parse(BacktrackingReader stream) throws IOException, RecognitionException {
        boolean success = false;
        ErrorTracer tracer = ErrorTracer.newTracer(getTracer());
        Context context = isMemoable() ? new MemoContext(stream, tracer) : new Context(stream, tracer);
        tracer.open(stream);
        try {
            success = getGrammar().getStart().reduce(context);
            if (!success) {
                throw tracer.newSyntaxException();
            }
            return buildResult();
        } finally {
            tracer.close(success);
        }
    }
    
    // Context
    
    private class Context implements ParseContext, ActionContext {
        
        final BacktrackingReader stream;
        final ErrorTracer tracer;
        
        final LinkedList<Transaction> txStack = new LinkedList<Transaction>();
        
        private Context(BacktrackingReader stream, ErrorTracer tracer) {
            this.stream = stream;
            this.tracer = tracer;
        }
        
        // ParseContext
        
        @Override
        public BacktrackingReader getStream() {
            return stream;
        }
        
        @Override
        public void startTransaction() {
            txStack.push(Parser.this.startTransaction());
        }
        
        @Override
        public void commitTransaction() {
            txStack.pop().commit();
        }
        
        @Override
        public void rollbackTransaction() {
            txStack.pop().rollback();
        }
        
        @Override
        public void storeTransaction(Production target) throws IOException {
            // nop
        }
        
        @Override
        public boolean restoreTransaction(Production target) throws IOException {
            return false;
        }
        
        @Override
        public boolean handleAction(Action action) throws ActionException {
            try {
                return Types.<ActionHandler<Parser<T>>>cast(action.getHandler()).handle(Parser.this, this);
            } catch (Throwable e) {
                throw new ActionException(action, e, stream.getEnd());
            }
        }
        
        @Override
        public void traceRule(Rule rule) throws IOException {
            tracer.trace(rule);
        }
        
        @Override
        public void backtraceRule(Rule rule, boolean success) throws IOException {
            tracer.backtrace(rule, success);
        }
        
        // ActionContext
        
        @Override
        public int length() {
            return stream.getLength();
        }
        
        @Override
        public char[] chars() {
            return stream.getChars();
        }
        
        @Override
        public String text() {
            return stream.getText();
        }
        
        @Override
        public Location start() {
            return stream.getStart();
        }
        
        @Override
        public Location end() {
            return stream.getEnd();
        }
        
    }
    
    private class MemoContext extends Context {
        
        final HashMap<Long, Transaction> txCache = new HashMap<Long, Transaction>();
        
        private MemoContext(BacktrackingReader stream, ErrorTracer tracer) {
            super(stream, tracer);
        }
        
        @Override
        public void storeTransaction(Production prod) throws IOException {
            Transaction tx = txStack.peek();
            int offset = stream.getStartOffset();
            tx.store(stream.getEndOffset() - offset);
            txCache.put(getCacheKey(prod, offset), tx);
        }
        
        @Override
        public boolean restoreTransaction(Production prod) throws IOException {
            Transaction tx = txCache.get(getCacheKey(prod, stream.getEndOffset()));
            if (tx != null) {
                stream.skip(tx.restore());
                return true;
            }
            return false;
        }
        
        private long getCacheKey(Production prod, long offset) {
            return (prod.getIndex() << 32) | offset;
        }
        
    }
    
}
