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

public final class Concatenation extends Expression.Nary implements Operator {
    
    Concatenation(Production owner, Expression[] children) {
        super(owner, children);
    }
    
    @Override
    public boolean reduce(ParseContext context) throws IOException, RecognitionException {
        context.stream().mark();
        context.tracer().onRuleTrace(this);
        for (int i = 0; i < children.length; i++) {
            if (!children[i].reduce(context)) {
                context.tracer().onRuleBacktrace(this, false);
                context.stream().reset();
                return false;
            }
        }
        context.tracer().onRuleBacktrace(this, true);
        context.stream().release();
        return true;
    }
    
    @Override
    public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    @Override
    public StringBuilder toString(StringBuilder buf, boolean debug) {
        toString(children[0], buf, children[0] instanceof Expression.Nary, debug);
        for (int i = 1; i < children.length; i++) {
            toString(children[i], buf.append(" "), children[i] instanceof Expression.Nary, debug);
        }
        return buf;
    }
    
}
