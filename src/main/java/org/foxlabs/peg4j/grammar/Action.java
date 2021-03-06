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

import org.foxlabs.common.text.CharBuffer;
import org.foxlabs.util.reflect.Types;

import org.foxlabs.peg4j.Parser;
import org.foxlabs.peg4j.ActionHandler;
import org.foxlabs.peg4j.ActionException;
import org.foxlabs.peg4j.RecognitionException;


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
  public boolean reduce(ParseContext context) throws IOException, RecognitionException {
    context.tracer().onRuleTrace(this);
    context.stream().mark();
    context.transaction().begin();
    if (child.reduce(context)) {
      if (handleAction(context)) {
        context.transaction().commit();
        context.stream().release();
        context.tracer().onRuleBacktrace(this, true);
        return true;
      }
    }
    context.transaction().rollback();
    context.stream().reset();
    context.tracer().onRuleBacktrace(this, false);
    return false;
  }

  private boolean handleAction(ParseContext context) throws IOException, RecognitionException {
    try {
      context.tracer().onBeforeAction(this);
      boolean result = Types.<ActionHandler<Parser<?>>>cast(handler).handle(context.parser(), context);
      context.tracer().onAfterAction(this, result);
      return result;
    } catch (Throwable e) {
      throw new ActionException(this, e, context.end());
    }
  }

  @Override
  public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
    visitor.visit(this);
  }

  @Override
  public CharBuffer toString(CharBuffer buf, boolean debug) {
    if (debug || !injected) {
      buf.append('$').append(name);
      return toString(child, buf, true, debug);
    } else {
      return toString(child, buf, child instanceof Expression.Nary, debug);
    }
  }

}
