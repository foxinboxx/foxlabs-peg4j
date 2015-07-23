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
    
    @Override
    public boolean reduce(ParseContext context) throws IOException, RecognitionException {
        context.stream().mark();
        context.tracer().trace(this);
        for (int i = 0; i < min; i++) {
            if (!child.reduce(context)) {
                context.tracer().backtrace(this, false);
                context.stream().reset();
                return false;
            }
        }
        for (int i = min; i < max; i++) {
            if (!child.reduce(context)) {
                context.tracer().backtrace(this, true);
                context.stream().reset();
                return true;
            }
        }
        context.tracer().backtrace(this, true);
        context.stream().release();
        return true;
    }
    
    @Override
    public final <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    @Override
    public void toString(StringBuilder buf, boolean debug) {
        toString(child, buf, child instanceof Operator, debug);
        buf.append('{');
        buf.append(min);
        if (min < max) {
            buf.append(',');
            if (max < Integer.MAX_VALUE) {
                buf.append(max);
            }
        }
        buf.append('}');
    }
    
    // OnceOrNone
    
    public static final class OnceOrNone extends Repetition {
        
        OnceOrNone(Production owner, Expression child) {
            super(owner, child, 0, 1);
        }
        
        @Override
        public Quantifier getQuantifier() {
            return Quantifier.ONCEORNONE;
        }
        
        @Override
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.stream().mark();
            context.tracer().trace(this);
            if (child.reduce(context)) {
                context.tracer().backtrace(this, true);
                context.stream().release();
            } else {
                context.tracer().backtrace(this, true);
                context.stream().reset();
            }
            return true;
        }
        
        @Override
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
        
        @Override
        public Quantifier getQuantifier() {
            return Quantifier.ZEROORMORE;
        }
        
        @Override
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.stream().mark();
            context.tracer().trace(this);
            while (child.reduce(context)) {
                context.stream().release();
                context.stream().mark();
            }
            context.tracer().backtrace(this, true);
            context.stream().reset();
            return true;
        }
        
        @Override
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
        
        @Override
        public Quantifier getQuantifier() {
            return Quantifier.ONCEORMORE;
        }
        
        @Override
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.stream().mark();
            context.tracer().trace(this);
            if (child.reduce(context)) {
                context.stream().release();
                context.stream().mark();
                while (child.reduce(context)) {
                    context.stream().release();
                    context.stream().mark();
                }
                context.tracer().backtrace(this, true);
                context.stream().reset();
                return true;
            }
            context.tracer().backtrace(this, false);
            context.stream().reset();
            return false;
        }
        
        @Override
        public void toString(StringBuilder buf, boolean debug) {
            toString(child, buf, child instanceof Operator, debug);
            buf.append(getQuantifier());
        }
        
    }
    
}
