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

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import java.io.IOException;

import org.foxlabs.peg4j.RecognitionException;

public final class Production extends Rule implements Comparable<Production> {
    
    private final String name;
    
    int index;
    Grammar grammar;
    Expression expression;
    
    final Set<Reference> references = new HashSet<Reference>();
    final Set<Production> referencedBy = new HashSet<Production>();
    
    boolean duplicated = false;
    
    Production(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public int getIndex() {
        return index;
    }
    
    public Grammar getGrammar() {
        return grammar;
    }
    
    public Expression getExpression() {
        return expression;
    }
    
    public Set<Reference> getReferences() {
        return Collections.unmodifiableSet(references);
    }
    
    public Set<Production> getReferencedBy() {
        return Collections.unmodifiableSet(referencedBy);
    }
    
    public boolean isStandalone() {
        int size = referencedBy.size();
        return size == 0 || size == 1 && referencedBy.contains(this);
    }
    
    public boolean isDuplicated() {
        return duplicated;
    }
    
    public boolean isUndefined() {
        return expression instanceof Terminal.Nil;
    }
    
    protected void findProblems(List<Problem> foundProblems) {
        foundProblems.addAll(getProblems());
        expression.findProblems(foundProblems);
    }
    
    @Override
    public boolean reduce(ParseContext context) throws IOException, RecognitionException {
        context.stream().mark();
        context.tracer().trace(this);
        if (expression.reduce(context)) {
            context.tracer().backtrace(this, true);
            context.stream().release();
            return true;
        }
        context.tracer().backtrace(this, false);
        context.stream().reset();
        return false;
    }
    
    @Override
    public <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
        visitor.visit(this);
    }
    
    @Override
    public int compareTo(Production other) {
        return index - other.index;
    }
    
    @Override
    public void toString(StringBuilder buf, boolean debug) {
        buf.append(name);
        buf.append(" : ");
        expression.toString(buf, debug);
    }
    
}
