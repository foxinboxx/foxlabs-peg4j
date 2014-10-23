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

import java.io.IOException;

import org.foxlabs.peg4j.RecognitionException;

public abstract class Exclusion extends Expression.Unary implements Operator {
    
    private Exclusion(Production owner, Expression child) {
        super(owner, child);
    }
    
    public abstract Predicate getPredicate();
    
    public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    public void toString(StringBuilder buf, boolean debug) {
        buf.append(getPredicate());
        toString(child, buf, child instanceof Operator, debug);
    }
    
    // Not
    
    public static final class Not extends Exclusion {
        
        Not(Production owner, Expression child) {
            super(owner, child);
        }
        
        public Predicate getPredicate() {
            return Predicate.NOT;
        }
        
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.getStream().mark();
            context.traceRule(this);
            if (child.reduce(context)) {
                context.backtraceRule(this, false);
                context.getStream().reset();
                return false;
            }
            context.backtraceRule(this, true);
            context.getStream().reset();
            return true;
        }
        
    }
    
    // And
    
    public static final class And extends Exclusion {
        
        And(Production owner, Expression child) {
            super(owner, child);
        }
        
        public Predicate getPredicate() {
            return Predicate.AND;
        }
        
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.getStream().mark();
            context.traceRule(this);
            if (child.reduce(context)) {
                context.backtraceRule(this, true);
                context.getStream().reset();
                return true;
            }
            context.backtraceRule(this, false);
            context.getStream().reset();
            return false;
        }
        
    }
    
}
