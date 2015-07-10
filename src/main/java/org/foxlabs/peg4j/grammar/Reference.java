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
    
    public boolean reduce(ParseContext context) throws IOException, RecognitionException {
        context.traceRule(this);
        context.getStream().mark();
        if (target.reduce(context)) {
            context.getStream().release();
            context.backtraceRule(this, true);
            return true;
        }
        context.getStream().reset();
        context.backtraceRule(this, false);
        return false;
    }
    
    public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    public void toString(StringBuilder buf, boolean debug) {
        buf.append(target.getName());
    }
    
    // Memo
    
    public static final class Memo extends Reference {
        
        Memo(Production owner, Production target) {
            super(owner, target);
        }
        
        public Modifier getModifier() {
            return Modifier.MEMO;
        }
        
        public boolean reduce(ParseContext context) throws IOException, RecognitionException {
            context.traceRule(this);
            if (context.restoreTransaction(target)) {
                context.backtraceRule(this, true);
                return true;
            }
            context.getStream().mark();
            context.startTransaction();
            if (target.reduce(context)) {
                context.storeTransaction(target);
                context.commitTransaction();
                context.getStream().release();
                context.backtraceRule(this, true);
                return true;
            }
            context.rollbackTransaction();
            context.getStream().reset();
            context.backtraceRule(this, false);
            return false;
        }
        
        public void toString(StringBuilder buf, boolean debug) {
            buf.append(getModifier()).append(target.getName());
        }
        
    }
    
}
