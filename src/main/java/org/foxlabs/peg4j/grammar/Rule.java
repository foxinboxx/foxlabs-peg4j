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
import java.util.LinkedList;
import java.util.Collections;

import java.io.IOException;

import org.foxlabs.peg4j.RecognitionException;

import org.foxlabs.util.Location;

/**
 * Base class for all rules.
 * 
 * @author Fox Mulder
 * @see Production
 * @see Expression
 * @see Terminal
 * @see Concatenation
 * @see Alternation
 * @see Repetition
 * @see Exclusion
 * @see Reference
 * @see Action
 */
public abstract class Rule {
    
    /**
     * Start location of this rule in character stream.
     */
    Location start = Location.UNKNOWN;
    
    /**
     * End location of this rule in character stream.
     */
    Location end = Location.UNKNOWN;
    
    /**
     * List of problems associated with this rule.
     */
    List<Problem> problems = new LinkedList<Problem>();
    
    /**
     * Constructs a new rule.
     */
    Rule() {
        super();
    }
    
    /**
     * Returns grammar containing this rule.
     * 
     * @return Grammar containing this rule.
     */
    public abstract Grammar getGrammar();
    
    /**
     * Returns start location of this rule in character stream.
     * 
     * @return Start location of this rule in character stream.
     */
    public final Location getStart() {
        return start;
    }
    
    /**
     * Returns end location of this rule in character stream.
     * 
     * @return End location of this rule in character stream.
     */
    public final Location getEnd() {
        return end;
    }
    
    /**
     * Returns source code of this rule if any or <code>null</code> if source
     * is not available.
     * 
     * @return Source code of this rule if any or <code>null</code> if source
     *         is not available.
     * @see Grammar#getSource(Location, Location)
     */
    public final String getSource() {
        return start.isUnknown() || end.isUnknown() ? null : getGrammar().getSource(start, end);
    }
    
    /**
     * Returns immutable list of problems associated with this rule.
     * 
     * @return Immutable list of problems associated with this rule.
     */
    public final List<Problem> getProblems() {
        return Collections.unmodifiableList(problems);
    }
    
    /**
     * Returns immutable list of problems associated with this rule and all
     * related rules.
     * 
     * @return Immutable list of problems associated with this rule and all
     *         related rules.
     */
    public final List<Problem> getAllProblems() {
        return Collections.unmodifiableList(RuleVisitor.ProblemCollector.collect(this));
    }
    
    /**
     * Performs reduce operation on this rule and returns <code>true</code> if
     * this rule matches character stream or produces semantic results.
     * 
     * @param context Parse context.
     * @return <code>true</code> if this rule matches character stream or
     *         produces semantic results; <code>false</code> otherwise.
     */
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
