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

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import org.foxlabs.peg4j.ActionHandler;
import org.foxlabs.peg4j.util.BacktrackingStack;

import org.foxlabs.util.Location;
import org.foxlabs.util.UnicodeSet;

// FIXME Refactor with ProductionBuilder, ... remove mark(), reset(), release()
// builder.production("sum")
//            .concat()
//                .tokenCS('a')
//                .any()
//            .choice()
public final class GrammarBuilder extends BacktrackingStack<Expression> {
    
    private final Map<String, Production> productionMap = new HashMap<String, Production>();
    private Production[] productions = new Production[64];
    private Production currentProduction = null;
    private int productionCount = 0;
    private int productionIndex = 0;
    
    private final Map<String, ActionHandler<?>> defaultHandlers;
    
    public GrammarBuilder() {
        this(null);
    }
    
    public GrammarBuilder(Map<String, ActionHandler<?>> handlers) {
        this.defaultHandlers = handlers;
    }
    
    @Override
    public GrammarBuilder mark() {
        super.mark();
        return this;
    }
    
    @Override
    public GrammarBuilder reset() {
        super.reset();
        return this;
    }
    
    @Override
    public GrammarBuilder release() {
        super.release();
        return this;
    }
    
    // FIXME public ProductionBuilder production(String name)
    public GrammarBuilder startProduction(String name) {
        return startProduction(name, Location.UNKNOWN);
    }
    
    // FIXME public ProductionBuilder production(String name, Location start)
    public GrammarBuilder startProduction(String name, Location start) {
        checkProductionInitiated(false);
        currentProduction = defineProduction(name, true);
        currentProduction.index = productionIndex++;
        currentProduction.start = Location.resolve(start);
        return this;
    }
    
    public GrammarBuilder endProduction() {
        return endProduction(Location.UNKNOWN);
    }
    
    public GrammarBuilder endProduction(Location end) {
        checkProductionInitiated(true);
        concat();
        currentProduction.expression = size() > 0 ? pop() : Terminal.nil(currentProduction);
        currentProduction.end = Location.resolve(end);
        currentProduction = null;
        return this;
    }
    
    private Production defineProduction(String name, boolean init) {
        Production prod = productionMap.get(name);
        if (init) {
            if (prod == null) {
                prod = createProduction(name);
                productionMap.put(name, prod);
            } else if (prod.expression != null) {
                prod.duplicated = true;
                prod = createProduction(name);
                prod.duplicated = true;
            }
        } else if (prod == null) {
             prod = createProduction(name);
             productionMap.put(name, prod);
        }
        return prod;
    }
    
    private Production createProduction(String name) {
        if (productionCount == productions.length) {
            Production[] copy = new Production[productions.length * 2 + 1];
            System.arraycopy(productions, 0, copy, 0, productions.length);
            productions = copy;
        }
        return productions[productionCount++] = new Production(name);
    }
    
    private void checkProductionInitiated(boolean init) {
        // FIXME This check can be avoided by ProductionBuilder class created
        // in startProduction() method
        if (currentProduction == null == init) {
            throw new IllegalStateException();
        }
    }
    
    // FIXME public ProductionBuilder any()
    public GrammarBuilder pushAny() {
        checkProductionInitiated(true);
        push(Terminal.any(currentProduction));
        return this;
    }
    
    // FIXME public ProductionBuilder tokenCS(char value)
    public GrammarBuilder pushTokenCS(char value) {
        return pushToken(value, true);
    }
    
    // FIXME public ProductionBuilder tokenIC(char value)
    public GrammarBuilder pushTokenIC(char value) {
        return pushToken(value, false);
    }
    
    // FIXME public ProductionBuilder token(char value, boolean cs)
    public GrammarBuilder pushToken(char value, boolean cs) {
        checkProductionInitiated(true);
        push(Terminal.tokenOf(currentProduction, value, cs));
        return this;
    }
    
    // FIXME public ProductionBuilder tokenCS(String value)
    public GrammarBuilder pushTokenCS(String value) {
        return pushToken(value, true);
    }
    
    // FIXME public ProductionBuilder tokenIC(String value)
    public GrammarBuilder pushTokenIC(String value) {
        return pushToken(value, false);
    }
    
    // FIXME public ProductionBuilder token(String value, boolean cs)
    public GrammarBuilder pushToken(String value, boolean cs) {
        checkProductionInitiated(true);
        push(Terminal.tokenOf(currentProduction, value, cs));
        return this;
    }
    
    // FIXME public ProductionBuilder tokensCS(String value)
    public GrammarBuilder pushTokensCS(String value) {
        return pushTokens(value, true);
    }
    
    // FIXME public ProductionBuilder tokensIC(String value)
    public GrammarBuilder pushTokensIC(String value) {
        return pushTokens(value, false);
    }
    
    // FIXME public ProductionBuilder tokens(String value, boolean cs)
    public GrammarBuilder pushTokens(String value, boolean cs) {
        checkProductionInitiated(true);
        int length = value.length();
        for (int i = 0; i < length; i++) {
            push(Terminal.tokenOf(currentProduction, value.charAt(i), cs));
        }
        return this;
    }
    
    // FIXME public ProductionBuilder interval(char min, char max)
    public GrammarBuilder pushInterval(char min, char max) {
        checkProductionInitiated(true);
        push(Terminal.intervalOf(currentProduction, min, max));
        return this;
    }
    
    // FIXME public ProductionBuilder set(char... values)
    public GrammarBuilder pushSet(char... values) {
        checkProductionInitiated(true);
        push(Terminal.setOf(currentProduction, values));
        return this;
    }
    
    // FIXME public ProductionBuilder set(UnicodeSet uset)
    public GrammarBuilder pushSet(UnicodeSet uset) {
        checkProductionInitiated(true);
        push(Terminal.setOf(currentProduction, uset));
        return this;
    }
    
    // FIXME public ProductionBuilder clazz(String name)
    public GrammarBuilder pushClass(String name) {
        checkProductionInitiated(true);
        push(Terminal.classOf(currentProduction, name));
        return this;
    }
    
    // FIXME public ProductionBuilder reference(String name)
    public GrammarBuilder pushReference(String name) {
        return pushReference(name, null);
    }
    
    // FIXME public ProductionBuilder clazz(String name, Modifier mod)
    public GrammarBuilder pushReference(String name, Modifier mod) {
        checkProductionInitiated(true);
        Production target = defineProduction(name, false);
        if (mod == null) {
            push(new Reference(currentProduction, target));
        } else {
            switch (mod) {
                case MEMO:
                    push(new Reference.Memo(currentProduction, target));
                    break;
            }
        }
        return this;
    }
    
    // FIXME public ProductionBuilder concat()
    public GrammarBuilder concat() {
        checkProductionInitiated(true);
        int size = size();
        if (size > 1) {
            push(new Concatenation(currentProduction, popAll(new Expression[size])));
        }
        return this;
    }
    
    // FIXME public ProductionBuilder choice()
    public GrammarBuilder choice() {
        checkProductionInitiated(true);
        int size = size();
        if (size > 1) {
            push(new Alternation(currentProduction, popAll(new Expression[size])));
        }
        return this;
    }
    
    // FIXME public ProductionBuilder repeat(int count)
    public GrammarBuilder repeat(int count) {
        return repeat(count, count);
    }
    
    // FIXME public ProductionBuilder repeat(int min, int max)
    public GrammarBuilder repeat(int min, int max) {
        checkProductionInitiated(true);
        if (max < min || min < 0 || max == 0) {
            throw new IllegalArgumentException();
        } else if (min == 0) {
            if (max == 1) {
                return repeat(Quantifier.ONCEORNONE);
            } else  if (max == Integer.MAX_VALUE) {
                return repeat(Quantifier.ZEROORMORE);
            }
        } else if (min == 1 && max == Integer.MAX_VALUE) {
            return repeat(Quantifier.ONCEORMORE);
        }
        push(new Repetition(currentProduction, pop(), min, max));
        return this;
    }
    
    // FIXME public ProductionBuilder repeat(Quantifier quant)
    public GrammarBuilder repeat(Quantifier quant) {
        checkProductionInitiated(true);
        switch (quant) {
            case ONCEORNONE:
                push(new Repetition.OnceOrNone(currentProduction, pop()));
                break;
            case ZEROORMORE:
                push(new Repetition.ZeroOrMore(currentProduction, pop()));
                break;
            case ONCEORMORE:
                push(new Repetition.OnceOrMore(currentProduction, pop()));
                break;
        }
        return this;
    }
    
    // FIXME public ProductionBuilder except(Predicate pred)
    public GrammarBuilder except(Predicate pred) {
        checkProductionInitiated(true);
        switch (pred) {
            case NOT:
                push(new Exclusion.Not(currentProduction, pop()));
                break;
            case AND:
                push(new Exclusion.And(currentProduction, pop()));
                break;
        }
        return this;
    }
    
    // FIXME public ProductionBuilder action(String name)
    public GrammarBuilder action(String name) {
        return action(name, defaultHandlers == null ? null : defaultHandlers.get(name));
    }
    
    // FIXME public ProductionBuilder action(String name, ActionHandler<?> handler)
    public GrammarBuilder action(String name, ActionHandler<?> handler) {
        checkProductionInitiated(true);
        if (isEmpty()) {
            push(Terminal.nil(currentProduction));
        }
        push(new Action(currentProduction, pop(), name, handler));
        return this;
    }
    
    // FIXME public ProductionBuilder start(Location start)
    public GrammarBuilder setStart(Location start) {
        peek().start = Location.resolve(start);
        return this;
    }
    
    // FIXME public ProductionBuilder end(Location end)
    public GrammarBuilder setEnd(Location end) {
        peek().end = Location.resolve(end);
        return this;
    }
    
    public Grammar buildGrammar() {
        return buildGrammar(new GrammarProblems(), null);
    }
    
    public Grammar buildGrammar(GrammarProblems problems) {
        return buildGrammar(problems, null);
    }
    
    public Grammar buildGrammar(GrammarProblems problems, String source) {
        checkProductionInitiated(false);
        Arrays.sort(productions, 0, productionCount);
        Production[] copy = new Production[productionCount];
        System.arraycopy(productions, 0, copy, 0, productionCount);
        Grammar grammar = new Grammar(copy, problems, source);
        GrammarCompiler.makeUndoInjections(grammar);
        return grammar;
    }
    
    public void clear() {
        super.clear();
        Arrays.fill(productions, 0, productionCount, null);
        productionMap.clear();
        currentProduction = null;
        productionCount = productionIndex = 0;
    }
    
}
