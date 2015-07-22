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
import java.util.LinkedList;
import java.util.Collections;

import java.io.IOException;

import org.foxlabs.peg4j.RecognitionException;

import org.foxlabs.util.Location;

public abstract class Rule {
    
    Location start = Location.UNKNOWN;
    Location end = Location.UNKNOWN;
    
    List<Problem> problems = null;
    
    Rule() {}
    
    public abstract Grammar getGrammar();
    
    public final Location getStart() {
        return start;
    }
    
    public final Location getEnd() {
        return end;
    }
    
    public final String getSource() {
        return getGrammar().getSource(start, end);
    }
    
    final void addProblem(Problem problem) {
        if (problems == null)
            problems = new LinkedList<Problem>();
        problems.add(problem);
    }
    
    public final List<Problem> getProblems() {
        if (problems == null)
            return Collections.emptyList();
        return Collections.unmodifiableList(problems);
    }
    
    public final List<Problem> getAllProblems() {
        List<Problem> problems = new ArrayList<Problem>();
        findProblems(problems);
        Collections.sort(problems);
        return problems;
    }
    
    protected abstract void findProblems(List<Problem> foundProblems);
    
    public abstract boolean reduce(ParseContext context) throws IOException, RecognitionException;
    
    public abstract <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E;
    
    public final String toString() {
        return toString(false);
    }
    
    public final String toString(boolean debug) {
        StringBuilder buf = new StringBuilder();
        toString(buf, debug);
        return buf.toString();
    }
    
    public final void toString(StringBuilder buf) {
        toString(buf, false);
    }
    
    public abstract void toString(StringBuilder buf, boolean debug);
    
    static void toString(Rule rule, StringBuilder buf, boolean parenthesize, boolean debug) {
        if (parenthesize) {
            buf.append('(');
            rule.toString(buf, debug);
            buf.append(')');
        } else {
            rule.toString(buf, debug);
        }
    }
    
}
