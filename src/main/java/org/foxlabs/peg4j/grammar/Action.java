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

import org.foxlabs.peg4j.ActionException;
import org.foxlabs.peg4j.ActionHandler;
import org.foxlabs.peg4j.Parser;
import org.foxlabs.peg4j.RecognitionException;
import org.foxlabs.util.reflect.Types;

public final class Action extends Expression.Unary {
    
    public static final String UNDO = "";
    
    private final String name;
    private final ActionHandler<?> handler;
    
    private boolean injected = false;
    
    Action(Production owner, Expression child) {
        this(owner, child, UNDO, ActionHandler.NOP);
        this.injected = true;
    }
    
    Action(Production owner, Expression child, String name, ActionHandler<?> handler) {
        super(owner, child);
        this.name = UNDO.equals(name) ? UNDO : name;
        this.handler = handler == null ? ActionHandler.NOP : handler;
    }
    
    public String getName() {
        return name;
    }
    
    public ActionHandler<?> getHandler() {
        return handler;
    }
    
    public boolean isUndo() {
        return name == UNDO;
    }
    
    public boolean isInjected() {
        return injected;
    }
    
    public boolean isUndefined() {
        return name != UNDO && handler == ActionHandler.NOP;
    }
    
    @Override
    public <P extends Parser<?>> boolean reduce(ParseContext<P> context) throws IOException, RecognitionException {
        context.tracer().trace(this);
        context.stream().mark();
        context.transaction().begin();
        if (child.reduce(context)) {
            if (handleAction(context)) {
                context.transaction().commit();
                context.stream().release();
                context.tracer().backtrace(this, true);
                return true;
            }
        }
        context.transaction().rollback();
        context.stream().reset();
        context.tracer().backtrace(this, false);
        return false;
    }
    
    private <P extends Parser<?>> boolean handleAction(ParseContext<P> context) throws RecognitionException {
        try {
            return Types.<ActionHandler<P>>cast(handler).handle(context.parser(), context);
        } catch (Throwable e) {
            throw new ActionException(this, e, context.end());
        }
    }
    
    @Override
    public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    @Override
    public void toString(StringBuilder buf, boolean debug) {
        if (debug || !injected) {
            buf.append('$').append(name);
            toString(child, buf, true, debug);
        } else {
            toString(child, buf, child instanceof Expression.Nary, debug);
        }
    }
    
}
