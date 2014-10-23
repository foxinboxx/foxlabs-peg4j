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

public abstract class Expression extends Rule {
    
    Production owner;
    Expression parent;
    
    Expression(Production owner) {
        this.owner = owner;
    }
    
    public final Grammar getGrammar() {
        return owner.getGrammar();
    }
    
    public final Production getOwner() {
        return owner;
    }
    
    public final Expression getParent() {
        return parent;
    }
    
    protected void findProblems(List<Problem> foundProblems) {
        foundProblems.addAll(getProblems());
    }
    
    // Unary
    
    public abstract static class Unary extends Expression {
        
        Expression child;
        
        Unary(Production owner, Expression child) {
            super(owner);
            this.child = child;
            child.parent = this;
        }
        
        public final Expression getChild() {
            return child;
        }
        
        protected void findProblems(List<Problem> foundProblems) {
            foundProblems.addAll(getProblems());
            child.findProblems(foundProblems);
        }
        
    }
    
    // Nary
    
    public abstract static class Nary extends Expression {
        
        Expression[] children;
        
        Nary(Production owner, Expression[] children) {
            super(owner);
            this.children = children;
            for (int i = 0; i < children.length; i++)
                children[i].parent = this;
        }
        
        public final int length() {
            return children.length;
        }
        
        public final Expression getChild(int index) {
            return children[index];
        }
        
        public final Expression[] getChildren() {
            return children.clone();
        }
        
        protected void findProblems(List<Problem> foundProblems) {
            foundProblems.addAll(getProblems());
            for (Expression child : children)
                child.findProblems(foundProblems);
        }
        
    }
    
}
