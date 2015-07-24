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

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

import org.foxlabs.peg4j.SyntaxException;
import org.foxlabs.peg4j.BacktrackingReader;
import org.foxlabs.peg4j.grammar.Rule;
import org.foxlabs.peg4j.grammar.Terminal;
import org.foxlabs.peg4j.grammar.Exclusion;
import org.foxlabs.peg4j.grammar.Reference;
import org.foxlabs.peg4j.grammar.Production;
import org.foxlabs.util.Location;

public class ErrorTracer extends RuleTracer.Adapter {
    
    private BacktrackingReader stream;
    
    private int errorOffset;
    private int errorLine;
    private int errorColumn;
    private int predicateLevel;
    
    private ReferenceNode referenceHead = null;
    
    private final Set<Terminal> expectedSet = new HashSet<Terminal>();
    
    public RuleTracer getWrappedTracer() {
        return null;
    }
    
    public Production getCurrentReference() {
        return referenceHead.reference.getTarget();
    }
    
    public Location getErrorLocation() {
        return Location.valueOf(stream.getFile(), errorLine, errorColumn);
    }
    
    public Set<Terminal> getExpectedSet() {
        return expectedSet;
    }
    
    public SyntaxException newSyntaxException() {
        return new SyntaxException(getExpectedSet(), getErrorLocation());
    }
    
    @Override
    public void open(BacktrackingReader stream) throws IOException {
        this.stream = stream;
        
        this.errorOffset = 0;
        this.errorLine = 0;
        this.errorColumn = 0;
        this.predicateLevel = 0;
        
        this.referenceHead = null;
        this.expectedSet.clear();
    }
    
    @Override
    public void trace(Rule rule) throws IOException {
        if (rule instanceof Reference) {
            referenceHead = new ReferenceNode((Reference) rule, referenceHead);
        } else if (rule instanceof Exclusion) {
            predicateLevel++;
        }
    }
    
    @Override
    public void backtrace(Rule rule, boolean success) throws IOException {
        if (rule instanceof Reference) {
            referenceHead = referenceHead.next;
        } else if (rule instanceof Exclusion) {
            predicateLevel--;
        } else if (rule instanceof Terminal && predicateLevel == 0 && !success) {
            int offset = stream.getStartOffset();
            if (offset >= errorOffset) {
                if (offset > errorOffset) {
                    expectedSet.clear();
                }
                errorOffset = offset;
                errorLine = stream.getStartLine();
                errorColumn = stream.getStartColumn();
                expectedSet.add((Terminal) rule);
            }
        }
    }
    
    // ReferenceNode
    
    private static class ReferenceNode {
        
        final Reference reference;
        final ReferenceNode next;
        
        private ReferenceNode(Reference reference, ReferenceNode next) {
            this.reference = reference;
            this.next = next;
        }
        
    }
    
    // Wrapper
    
    private static class Wrapper extends ErrorTracer {
        
        private final RuleTracer tracer;
        
        private Wrapper(RuleTracer tracer) {
            this.tracer = tracer;
        }
        
        @Override
        public RuleTracer getWrappedTracer() {
            return tracer;
        }
        
        @Override
        public void open(BacktrackingReader stream) throws IOException {
            super.open(stream);
            tracer.open(stream);
        }
        
        @Override
        public void trace(Rule rule) throws IOException {
            super.trace(rule);
            tracer.trace(rule);
        }
        
        @Override
        public void backtrace(Rule rule, boolean success) throws IOException {
            super.backtrace(rule, success);
            tracer.backtrace(rule, success);
        }
        
        @Override
        public void lookup(Reference reference, boolean hit) throws IOException {
            super.lookup(reference, hit);
            tracer.lookup(reference, hit);
        }

        @Override
        public void cache(Reference reference) throws IOException {
            super.cache(reference);
            tracer.cache(reference);
        }
        
        @Override
        public void close(boolean result) throws IOException {
            super.close(result);
            tracer.close(result);
        }
        
    }
    
    public static ErrorTracer newTracer(RuleTracer tracer) {
        if (tracer == null) {
            return new ErrorTracer();
        } else if (tracer instanceof ErrorTracer) {
            return (ErrorTracer) tracer;
        } else {
            return new ErrorTracer.Wrapper(tracer);
        }
    }
    
}
