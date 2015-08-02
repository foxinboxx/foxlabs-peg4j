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

import java.io.IOException;

import org.foxlabs.peg4j.BacktrackingReader;
import org.foxlabs.peg4j.grammar.Rule;
import org.foxlabs.peg4j.grammar.Reference;

public interface RuleTracer {
    
    void open(BacktrackingReader stream) throws IOException;
    
    void onTrace(Rule rule) throws IOException;
    
    void onBacktrace(Rule rule, boolean success) throws IOException;
    
    void onLookup(Reference reference, boolean hit) throws IOException;
    
    void onCache(Reference reference) throws IOException;
    
    void close(boolean result) throws IOException;
    
    // Adapter
    
    class Adapter implements RuleTracer {
        
        @Override
        public void open(BacktrackingReader stream) throws IOException {}
        
        @Override
        public void onTrace(Rule rule) throws IOException {}
        
        @Override
        public void onBacktrace(Rule rule, boolean success) throws IOException {}
        
        @Override
        public void onLookup(Reference reference, boolean hit) throws IOException {}
        
        @Override
        public void onCache(Reference reference) throws IOException {}
        
        @Override
        public void close(boolean result) throws IOException {}
        
    }
    
    // Wrapper
    
    class Wrapper implements RuleTracer {
        
        protected final RuleTracer tracer;
        
        public Wrapper(RuleTracer tracer) {
            this.tracer = tracer;
        }
        
        @Override
        public void open(BacktrackingReader stream) throws IOException {
            tracer.open(stream);
        }
        
        @Override
        public void onTrace(Rule rule) throws IOException {
            tracer.onTrace(rule);
        }
        
        @Override
        public void onBacktrace(Rule rule, boolean success) throws IOException {
            tracer.onBacktrace(rule, success);
        }
        
        @Override
        public void onLookup(Reference reference, boolean hit) throws IOException {
            tracer.onLookup(reference, hit);
        }
        
        @Override
        public void onCache(Reference reference) throws IOException {
            tracer.onCache(reference);
        }
        
        @Override
        public void close(boolean result) throws IOException {
            tracer.close(result);
        }
        
    }
    
    // Chain
    
    class Chain implements RuleTracer {
        
        protected final RuleTracer[] chain;
        
        public Chain(RuleTracer... chain) {
            this.chain = chain;
        }
        
        @Override
        public void open(BacktrackingReader stream) throws IOException {
            for (RuleTracer tracer : chain) {
                tracer.open(stream);
            }
        }
        
        @Override
        public void onTrace(Rule rule) throws IOException {
            for (RuleTracer tracer : chain) {
                tracer.onTrace(rule);
            }
        }
        
        @Override
        public void onBacktrace(Rule rule, boolean success) throws IOException {
            for (RuleTracer tracer : chain) {
                tracer.onBacktrace(rule, success);
            }
        }
        
        @Override
        public void onLookup(Reference reference, boolean hit) throws IOException {
            for (RuleTracer tracer : chain) {
                tracer.onLookup(reference, hit);
            }
        }
        
        @Override
        public void onCache(Reference reference) throws IOException {
            for (RuleTracer tracer : chain) {
                tracer.onCache(reference);
            }
        }
        
        @Override
        public void close(boolean result) throws IOException {
            for (RuleTracer tracer : chain) {
                tracer.close(result);
            }
        }
        
    }
    
}
