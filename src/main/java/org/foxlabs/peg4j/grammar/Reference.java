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

import org.foxlabs.peg4j.Parser;
import org.foxlabs.peg4j.RecognitionException;

public class Reference extends Expression {
    
    Production target;
    
    Reference(Production owner, Production target) {
        super(owner);
        this.target = target;
        owner.references.add(this);
        target.referencedBy.add(owner);
    }
    
    public final Production getTarget() {
        return target;
    }
    
    public final String getTargetName() {
        return target.getName();
    }
    
    public Modifier getModifier() {
        return null;
    }
    
    @Override
    public <P extends Parser<?>> boolean reduce(ParseContext<P> context)
            throws IOException, RecognitionException {
        context.tracer().trace(this);
        context.stream().mark();
        if (target.reduce(context)) {
            context.stream().release();
            context.tracer().backtrace(this, true);
            return true;
        }
        context.stream().reset();
        context.tracer().backtrace(this, false);
        return false;
    }
    
    @Override
    public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    @Override
    public void toString(StringBuilder buf, boolean debug) {
        buf.append(target.getName());
    }
    
    // Memo
    
    public static final class Memo extends Reference {
        
        Memo(Production owner, Production target) {
            super(owner, target);
        }
        
        @Override
        public Modifier getModifier() {
            return Modifier.MEMO;
        }
        
        @Override
        public <P extends Parser<?>> boolean reduce(ParseContext<P> context)
                throws IOException, RecognitionException {
            context.tracer().trace(this);
            context.stream().mark();
            for (int length = context.transaction().load(id(context)); length >= 0;) {
                context.stream().release();
                context.stream().skip(length);
                context.tracer().backtrace(this, true);
                return true;
            }
            context.transaction().begin();
            if (target.reduce(context)) {
                context.transaction().save(id(context), context.stream().getLength());
                context.transaction().commit();
                context.stream().release();
                context.tracer().backtrace(this, true);
                return true;
            }
            context.transaction().rollback();
            context.stream().reset();
            context.tracer().backtrace(this, false);
            return false;
        }
        
        private long id(ParseContext<?> context) {
            return ((long) target.getIndex() << 32) | context.stream().getStartOffset();
        }
        
        @Override
        public void toString(StringBuilder buf, boolean debug) {
            buf.append(getModifier()).append(target.getName());
        }
        
    }
    
}
