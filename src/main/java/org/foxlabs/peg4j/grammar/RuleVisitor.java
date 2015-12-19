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
import java.util.ArrayList;
import java.util.Collections;

public interface RuleVisitor<E extends Throwable> {
    
    void visit(Terminal rule) throws E;
    
    void visit(Production rule) throws E;
    
    void visit(Reference rule) throws E;
    
    void visit(Action rule) throws E;
    
    void visit(Concatenation rule) throws E;
    
    void visit(Alternation rule) throws E;
    
    void visit(Repetition rule) throws E;
    
    void visit(Exclusion rule) throws E;
    
    // Adapter
    
    public static class Adapter<E extends Throwable> implements RuleVisitor<E> {
        
        public void visit(Terminal rule) throws E {}
        
        public void visit(Production rule) throws E {}
        
        public void visit(Reference rule) throws E {}
        
        public void visit(Action rule) throws E {}
        
        public void visit(Concatenation rule) throws E {}
        
        public void visit(Alternation rule) throws E {}
        
        public void visit(Repetition rule) throws E {}
        
        public void visit(Exclusion rule) throws E {}
        
    }
    
    // ProblemCollector
    
    public static class ProblemCollector implements RuleVisitor<RuntimeException> {
        
        private final List<Problem> problems;
        
        public ProblemCollector() {
            this(new ArrayList<Problem>());
        }
        
        public ProblemCollector(List<Problem> problems) {
            this.problems = problems;
        }
        
        public List<Problem> getProblems() {
            Collections.sort(problems);
            return problems;
        }
        
        @Override
        public void visit(Terminal rule) {
            problems.addAll(rule.getProblems());
        }
        
        @Override
        public void visit(Production rule) {
            problems.addAll(rule.getProblems());
            rule.getExpression().accept(this);
        }
        
        @Override
        public void visit(Reference rule) {
            problems.addAll(rule.getProblems());
        }
        
        @Override
        public void visit(Action rule) {
            problems.addAll(rule.getProblems());
            rule.getChild().accept(this);
        }
        
        @Override
        public void visit(Concatenation rule) {
            problems.addAll(rule.getProblems());
            for (int i = 0, count = rule.length(); i < count; i++) {
                rule.getChild(i).accept(this);
            }
        }
        
        @Override
        public void visit(Alternation rule) {
            problems.addAll(rule.getProblems());
            for (int i = 0, count = rule.length(); i < count; i++) {
                rule.getChild(i).accept(this);
            }
        }
        
        @Override
        public void visit(Repetition rule) {
            problems.addAll(rule.getProblems());
            rule.getChild().accept(this);
        }
        
        @Override
        public void visit(Exclusion rule) {
            problems.addAll(rule.getProblems());
            rule.getChild().accept(this);
        }
        
        public static List<Problem> collect(Rule rule) {
            ProblemCollector collector = new ProblemCollector();
            rule.accept(collector);
            return collector.getProblems();
        }
        
    }
    
}
