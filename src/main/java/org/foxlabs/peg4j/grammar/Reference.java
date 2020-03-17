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
  public boolean reduce(ParseContext context) throws IOException, RecognitionException {
    context.tracer().onRuleTrace(this);
    context.stream().mark();
    if (target.reduce(context)) {
      context.stream().release();
      context.tracer().onRuleBacktrace(this, true);
      return true;
    }
    context.stream().reset();
    context.tracer().onRuleBacktrace(this, false);
    return false;
  }

  @Override
  public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
    visitor.visit(this);
  }

  @Override
  public CharBuffer toString(CharBuffer buf, boolean debug) {
    return buf.append(target.getName());
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
    public boolean reduce(ParseContext context) throws IOException, RecognitionException {
      if (context.parser().isMemoable()) {
        context.tracer().onRuleTrace(this);
        context.stream().mark();
        if (context.transaction().load()) {
          context.tracer().onCacheGet(this, true);
          context.stream().release();
          context.tracer().onRuleBacktrace(this, true);
          return true;
        } else {
          context.tracer().onCacheGet(this, false);
        }
        context.transaction().begin();
        if (target.reduce(context)) {
          if (context.transaction().save() != null) {
            context.tracer().onCachePut(this);
          }
          context.transaction().commit();
          context.stream().release();
          context.tracer().onRuleBacktrace(this, true);
          return true;
        }
        context.transaction().rollback();
        context.stream().reset();
        context.tracer().onRuleBacktrace(this, false);
        return false;
      } else {
        return super.reduce(context);
      }
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      return buf.append(getModifier().toString()).append(target.getName());
    }

  }

}
