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

public class Repetition extends Expression.Unary implements Operator {
    
    private final int min;
    private final int max;
    
    Repetition(Production owner, Expression child, int min, int max) {
        super(owner, child);
        this.min = min;
        this.max = max;
    }
    
    public final int getMin() {
        return min;
    }
    
    public final int getMax() {
        return max;
    }
    
    public Quantifier getQuantifier() {
        return null;
    }
    
    public boolean reduce(ParseContext context) throws IOException, RecognitionException {
        context.getStream().mark();
        context.traceRule(this);
        for (int i = 0; i < min; i++)
            if (!child.reduce(context)) {
                context.backtraceRule(this, false);
                context.getStream().reset();
                return false;
            }
        for (int i = min; i < max; i++)
            if (!child.reduce(context)) {
                context.backtraceRule(this, true);
                context.getStream().reset();
                return true;
            }
        context.backtraceRule(this, true);
        context.getStream().release();
        return true;
    }
    
    public final <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    public void toString(StringBuilder buf, boolean debug) {
        toString(child, buf, child instanceof Operator, debug);
        buf.append('{');
        buf.append(min);
        if (min < max) {
            buf.append(',');
            if (max < Integer.MAX_VALUE)
                buf.append(max);
        }
        buf.append('}');
    }
    
    // OnceOrNone
    
    public static final class OnceOrNone extends Repetition {
        
        OnceOrNone(Production owner, Expression child) {
            super(owner, child, 0, 1);
        }
        
        public Quantifier getQuantifier() {
            return Quantifier.ONCEORNONE;
        }
        
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.getStream().mark();
            context.traceRule(this);
            if (child.reduce(context)) {
                context.backtraceRule(this, true);
                context.getStream().release();
            } else {
                context.backtraceRule(this, true);
                context.getStream().reset();
            }
            return true;
        }
        
        public void toString(StringBuilder buf, boolean debug) {
            toString(child, buf, child instanceof Operator, debug);
            buf.append(getQuantifier());
        }
        
    }
    
    // ZeroOrMore
    
    public static final class ZeroOrMore extends Repetition {
        
        ZeroOrMore(Production owner, Expression child) {
            super(owner, child, 0, Integer.MAX_VALUE);
        }
        
        public Quantifier getQuantifier() {
            return Quantifier.ZEROORMORE;
        }
        
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.getStream().mark();
            context.traceRule(this);
            while (child.reduce(context)) {
                context.getStream().release();
                context.getStream().mark();
            }
            context.backtraceRule(this, true);
            context.getStream().reset();
            return true;
        }
        
        public void toString(StringBuilder buf, boolean debug) {
            toString(child, buf, child instanceof Operator, debug);
            buf.append(getQuantifier());
        }
        
    }
    
    // OnceOrMore
    
    public static final class OnceOrMore extends Repetition {
        
        OnceOrMore(Production owner, Expression child) {
            super(owner, child, 1, Integer.MAX_VALUE);
        }
        
        public Quantifier getQuantifier() {
            return Quantifier.ONCEORMORE;
        }
        
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.getStream().mark();
            context.traceRule(this);
            if (child.reduce(context)) {
                context.getStream().release();
                context.getStream().mark();
                while (child.reduce(context)) {
                    context.getStream().release();
                    context.getStream().mark();
                }
                context.backtraceRule(this, true);
                context.getStream().reset();
                return true;
            }
            context.backtraceRule(this, false);
            context.getStream().reset();
            return false;
        }
        
        public void toString(StringBuilder buf, boolean debug) {
            toString(child, buf, child instanceof Operator, debug);
            buf.append(getQuantifier());
        }
        
    }
    
}
