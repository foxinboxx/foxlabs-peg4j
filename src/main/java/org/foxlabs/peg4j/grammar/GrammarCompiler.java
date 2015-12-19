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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Arrays;

import static org.foxlabs.peg4j.grammar.Problem.Code.*;

public final class GrammarCompiler {
    
    public static final int SUPPRESS_WARNINGS = 0x01;
    public static final int SUPPRESS_HINTS = 0x02;
    
    private GrammarCompiler() {
        super();
    }
    
    public static void compile(Grammar grammar) {
        compile(grammar, 0);
    }
    
    public static void compile(Grammar grammar, int flags) {
        if (grammar.getProductionCount() > 0) {
            synchronized (grammar) {
                GrammarProblems unbindedProblems = new GrammarProblems();
                for (Problem problem : grammar.getProblems().getProblems()) {
                    if (problem.getRule() == null) {
                        unbindedProblems.add(problem);
                    }
                }
                
                grammar.getProblems().clear();
                grammar.getProblems().addAll(unbindedProblems);
                
                boolean sw = (flags & SUPPRESS_WARNINGS) != 0;
                boolean sh = (flags & SUPPRESS_HINTS) != 0;
                
                new LocalAnalyzer(grammar).findProblems(sw, sh);
                new RecursionFinder(grammar).findProblems();
                
                grammar.getProblems().sort();
            }
        }
    }
    
    // LocalAnalyzer
    
    static final class LocalAnalyzer implements RuleVisitor<RuntimeException> {
        
        final Grammar grammar;
        
        boolean sw, sh;
        
        LocalAnalyzer(Grammar grammar) {
            this.grammar = grammar;
        }
        
        public void findProblems(boolean sw, boolean sh) {
            this.sw = sw;
            this.sh = sh;
            
            int count = grammar.getProductionCount();
            for (int i = 0; i < count; i++) {
                grammar.getProduction(i).accept(this);
            }
        }
        
        public void visit(Terminal rule) {
            if (!(rule instanceof Terminal.Nil)) {
                if (rule.isEmpty()) {
                    grammar.getProblems().add(EMPTY_TERMINAL, rule);
                } else if (rule instanceof Terminal.Class) {
                    Terminal.Class term = (Terminal.Class) rule;
                    if (term.isUndefined()) {
                        grammar.getProblems().add(UNSUPPORTED_CLASS, term, term.getName());
                    }
                } else if (!sh && rule.isInefficient()) {
                    grammar.getProblems().add(INEFFICIENT_TERMINAL, rule);
                }
            }
        }
        
        public void visit(Production rule) {
            if (rule.isDuplicated()) {
                grammar.getProblems().add(DUPLICATE_PRODUCTION, rule, rule.getName());
            }
            
            if (!sw) {
                if (rule.isStandalone() && rule != grammar.getStart()) {
                    grammar.getProblems().add(UNUSED_PRODUCTION, rule, rule.getName());
                }
            }
            
            rule.expression.accept(this);
        }
        
        public void visit(Reference rule) {
            if (rule.target.isUndefined()) {
                grammar.getProblems().add(UNDEFINED_PRODUCTION, rule, rule.getTargetName());
            }
        }
        
        public void visit(Action rule) {
            if (!sw) {
                if (rule.isUndefined()) {
                    grammar.getProblems().add(UNDEFINED_ACTION, rule, rule.getName());
                }
            }
            
            rule.child.accept(this);
        }
        
        public void visit(Concatenation rule) {
            int length = rule.children.length;
            for (int i = 0; i < length; i++) {
                rule.children[i].accept(this);
            }
            
            if (!sh) {
                // FIXME We can find more rules and report their numbers
                Rule rule1 = rule.children[0];
                for (int i = 1; i < length; i++) {
                    Rule rule2 = rule.children[i];
                    if (rule1 instanceof Terminal.Token && rule2 instanceof Terminal.Token) {
                        Terminal.Token term1 = (Terminal.Token) rule1;
                        Terminal.Token term2 = (Terminal.Token) rule2;
                        if (term1.isCaseSensitive() == term2.isCaseSensitive()) {
                            grammar.getProblems().add(INEFFICIENT_CONCATENATION, rule);
                            break;
                        }
                    }
                    rule1 = rule2;
                }
            }
        }
        
        public void visit(Alternation rule) {
            int length = rule.children.length;
            for (int i = 0; i < length; i++) {
                rule.children[i].accept(this);
            }
            
            if (!sh) {
                // FIXME We can find more rules and report their numbers
                Rule rule1 = rule.children[0];
                for (int i = 1; i < length; i++) {
                    Rule rule2 = rule.children[i];
                    if (rule1 instanceof Terminal && rule2 instanceof Terminal) {
                        Terminal term1 = (Terminal) rule1;
                        Terminal term2 = (Terminal) rule2;
                        if (term1.isDetermined() && term2.isDetermined()) {
                            grammar.getProblems().add(INEFFICIENT_ALTERNATION, rule);
                            break;
                        }
                    }
                    rule1 = rule2;
                }
            }
        }
        
        public void visit(Repetition rule) {
            rule.child.accept(this);
        }
        
        public void visit(Exclusion rule) {
            rule.child.accept(this);
        }
        
    }
    
    // RecursionFinder
    
    static final class RecursionFinder implements RuleVisitor<RuntimeException> {
        
        final Grammar grammar;
        final Boolean[] consumingFlags;
        final LinkedList<String> referencePath;
        
        Production source;
        boolean consuming;
        boolean recursion;
        
        RecursionFinder(Grammar grammar) {
            this.grammar = grammar;
            this.consumingFlags = new Boolean[grammar.getProductionCount()];
            this.referencePath = new LinkedList<String>();
        }
        
        public void findProblems() {
            int count = grammar.getProductionCount();
            for (int i = 0; i < count; i++) {
                Production rule = grammar.getProduction(i);
                reset(rule);
                rule.accept(this);
            }
        }
        
        public void visit(Terminal rule) {
            consuming = !(rule instanceof Terminal.Nil);
        }
        
        public void visit(Production rule) {
            int index = rule.getIndex();
            if (consumingFlags[index] == null) {
                boolean consume = consuming;
                consuming = false;
                referencePath.addLast(rule.getName());
                rule.expression.accept(this);
                referencePath.removeLast();
                consumingFlags[index] = consuming;
                consuming = consume;
            }
            consuming |= consumingFlags[index];
        }
        
        public void visit(Reference rule) {
            if (source == rule.target) {
                if (!consuming) {
                    grammar.getProblems().add(LEFT_RECURSION, rule, rule.getTargetName(), referencePath.toString());
                    recursion = true;
                }
            } else if (!recursion) {
                rule.target.accept(this);
            }
        }
        
        public void visit(Action rule) {
            if (!recursion) {
                rule.child.accept(this);
            }
        }
        
        public void visit(Concatenation rule) {
            int length = rule.children.length;
            for (int i = 0; i < length && !(recursion || consuming); i++) {
                rule.children[i].accept(this);
            }
        }
        
        public void visit(Alternation rule) {
            boolean consume = true;
            int length = rule.children.length;
            for (int i = 0; i < length && !recursion; i++) {
                consuming = false;
                rule.children[i].accept(this);
                consume &= consuming;
            }
            consuming = consume;
        }
        
        public void visit(Repetition rule) {
            if (!recursion) {
                rule.child.accept(this);
                if (rule.getMin() == 0) {
                    consuming = false;
                }
            }
        }
        
        public void visit(Exclusion rule) {
            if (!recursion) {
                rule.child.accept(this);
            }
        }
        
        public void reset(Production start) {
            source = start;
            consuming = false;
            recursion = false;
            referencePath.clear();
        }
        
    }
    
    // MemoInjector
    
    public static void makeMemoInjections(Grammar grammar) {
        MemoInjector injector = new MemoInjector();
        int count = grammar.getProductionCount();
        for (int i = 0; i < count; i++) {
            grammar.getProduction(i).expression.accept(injector);
        }
    }
    
    static final class MemoInjector extends RuleVisitor.Adapter<RuntimeException> {
        
        public void visit(Action rule) {
            visit((Expression.Unary) rule);
        }
        
        public void visit(Concatenation rule) {
            visit((Expression.Nary) rule);
        }
        
        public void visit(Alternation rule) {
            visit((Expression.Nary) rule);
        }
        
        public void visit(Repetition rule) {
            visit((Expression.Unary) rule);
        }
        
        public void visit(Exclusion rule) {
            visit((Expression.Unary) rule);
        }
        
        private void visit(Expression.Unary rule) {
            Expression operand = rule.child;
            if (operand instanceof Reference) {
                Reference ref = (Reference) operand;
                if (ref.getModifier() == null) {
                    rule.child = copyRef(ref);
                }
            } else {
                operand.accept(this);
            }
        }
        
        private void visit(Expression.Nary rule) {
            int length = rule.children.length;
            for (int i = 0; i < length; i++) {
                Expression operand = rule.children[i];
                if (operand instanceof Reference) {
                    Reference ref = (Reference) operand;
                    if (ref.getModifier() == null) {
                        rule.children[i] = copyRef(ref);
                    }
                } else {
                    operand.accept(this);
                }
            }
        }
        
        private Reference copyRef(Reference oldRef) {
            Reference newRef = new Reference.Memo(oldRef.owner, oldRef.target);
            newRef.parent = oldRef.parent;
            newRef.owner.references.remove(oldRef);
            newRef.owner.references.add(newRef);
            newRef.start = oldRef.start;
            newRef.end = oldRef.end;
            newRef.problems = oldRef.problems;
            return newRef;
        }
        
    }
    
    // UndoInjector
    
    static void makeUndoInjections(Grammar grammar) {
        (new UndoInjector(grammar)).makeInjections();
    }
    
    static final class UndoInjector implements RuleVisitor<RuntimeException> {
        
        final Production[] productions;
        
        final boolean[] untestedFlags;
        final Boolean[] modifyMarks;
        
        boolean injection = false;
        Boolean currentModify = null;
        
        UndoInjector(Grammar grammar) {
            this.productions = grammar.getProductions();
            this.untestedFlags = new boolean[productions.length];
            this.modifyMarks = new Boolean[productions.length];
        }
        
        public void makeInjections() {
            LinkedList<Production> unresolvedList = new LinkedList<Production>();
            int count = productions.length;
            for (int i = 0; i < count; i++) {
                unresolvedList.add(productions[i]);
            }
            
            boolean resolving;
            injection = false;
            do {
                reset();
                resolving = false;
                Iterator<Production> i = unresolvedList.iterator();
                while (i.hasNext()) {
                    Production rule = i.next();
                    rule.accept(this);
                    if (modifyMarks[rule.index] != null) {
                        i.remove();
                        resolving = true;
                    }
                }
            } while (resolving && unresolvedList.size() > 0);
            
            for (Production rule : unresolvedList) {
                modifyMarks[rule.index] = Boolean.FALSE;
            }
            
            injection = true;
            for (int i = 0; i < count; i++) {
                if (modifyMarks[i] == Boolean.TRUE) {
                    productions[i].accept(this);
                }
            }
        }
        
        public void visit(Terminal rule) {
            currentModify = Boolean.FALSE;
        }
        
        public void visit(Production rule) {
            if (injection) {
                rule.expression.accept(this);
            } else {
                int index = rule.index;
                currentModify = modifyMarks[index];
                if (currentModify == null && untestedFlags[index]) {
                    untestedFlags[index] = false;
                    rule.expression.accept(this);
                    modifyMarks[index] = currentModify;
                }
            }
        }
        
        public void visit(Reference rule) {
            if (injection) {
                currentModify = modifyMarks[rule.target.index];
            } else {
                rule.target.accept(this);
            }
        }
        
        public void visit(Action rule) {
            rule.child.accept(this);
            currentModify = Boolean.TRUE;
        }
        
        public void visit(Concatenation rule) {
            Boolean modify = Boolean.FALSE;
            int length = rule.children.length;
            int lastIndex = length - 1;
            for (int i = 0; i < lastIndex; i++) {
                rule.children[i].accept(this);
                modify = merge(modify, currentModify);
            }
            if (injection && modify == Boolean.TRUE) {
                doInject(rule);
            }
            rule.children[lastIndex].accept(this);
            currentModify = merge(modify, currentModify);
        }
        
        public void visit(Alternation rule) {
            Boolean modify = Boolean.FALSE;
            int length = rule.children.length;
            for (int i = 0; i < length; i++) {
                rule.children[i].accept(this);
                modify = merge(modify, currentModify);
            }
            currentModify = modify;
        }
        
        public void visit(Repetition rule) {
            rule.child.accept(this);
        }
        
        public void visit(Exclusion rule) {
            rule.child.accept(this);
            if (injection && currentModify == Boolean.TRUE) {
                doInject(rule);
            }
            currentModify = Boolean.FALSE;
        }
        
        private void doInject(Expression expr) {
            if (!(expr.parent instanceof Action)) {
                Expression parent = expr.parent;
                Action undo = new Action(expr.owner, expr);
                undo.parent = parent;
                undo.start = expr.start;
                undo.end = expr.end;
                if (parent == null) {
                    expr.owner.expression = undo;
                } else if (parent instanceof Expression.Unary) {
                    Expression.Unary unary = (Expression.Unary) parent;
                    unary.child = undo;
                } else if (parent instanceof Expression.Nary) {
                    Expression.Nary nary = (Expression.Nary) parent;
                    int length = nary.children.length;
                    for (int i = 0; i < length; i++) {
                        if (nary.children[i] == expr) {
                            nary.children[i] = undo;
                            break;
                        }
                    }
                }
            }
        }
        
        private void reset() {
            Arrays.fill(untestedFlags, true);
        }
        
    }
    
    static Boolean merge(Boolean x, Boolean y) {
        if (x == Boolean.TRUE || y == Boolean.TRUE) {
            return Boolean.TRUE;
        } else if (x == null || y == null) {
            return null;
        } else {
            return Boolean.FALSE;
        }
    }
    
}
