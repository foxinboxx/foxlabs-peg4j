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

import java.io.Serializable;

import org.foxlabs.peg4j.resource.ResourceManager;

import org.foxlabs.util.Location;

/**
 * Defines a problem in a grammar. There are four types of problems defined
 * (<code>FATAL</code>, <code>ERROR</code>, <code>WARNING</code> and
 * <code>HINT</code>). Also each problem has associated code, location in
 * character stream and rule if rule is the cause of the problem. All possible
 * problem codes are listed in the {@link Problem.Code} enumeration.
 * 
 * @author Fox Mulder
 * @see GrammarProblems
 * @see GrammarCompiler
 * @see GrammarParser
 */
public final class Problem implements Serializable, Comparable<Problem> {
    private static final long serialVersionUID = -4976957943431107554L;
    
    /**
     * Code of this problem.
     */
    private final Code code;
    
    /**
     * Start location of this problem in character stream.
     */
    private final Location start;
    
    /**
     * End location of this problem in character stream.
     */
    private final Location end;
    
    /**
     * Attributes that will be substituted in this problem description message.
     */
    private final String[] attributes;
    
    /**
     * Rule that is the cause of this problem.
     */
    private transient Rule rule;
    
    /**
     * Constructs a new problem with the specified code and source location.
     * 
     * @param code Code of this problem.
     * @param start Start location of this problem in character stream.
     * @param end End location of this problem in character stream.
     */
    Problem(Code code, Location start, Location end) {
        this(code, start, end, null, (String[]) null);
    }
    
    /**
     * Constructs a new problem with the specified code, source location and
     * attributes.
     * 
     * @param code Code of this problem.
     * @param start Start location of this problem in character stream.
     * @param end End location of this problem in character stream.
     * @param attributes Attributes that will be substituted in this problem
     *        description message.
     */
    Problem(Code code, Location start, Location end, String... attributes) {
        this(code, start, end, null, attributes);
    }
    
    /**
     * Constructs a new problem with the specified code and rule.
     * 
     * @param code Code of this problem.
     * @param rule Rule that is the cause of this problem.
     */
    Problem(Code code, Rule rule) {
        this(code, rule.getStart(), rule.getEnd(), rule, (String[]) null);
    }
    
    /**
     * Constructs a new problem with the specified code, rule and attributes.
     * 
     * @param code Code of this problem.
     * @param rule Rule that is the cause of this problem.
     * @param attributes Attributes that will be substituted in this problem
     *        description message.
     */
    Problem(Code code, Rule rule, String... attributes) {
        this(code, rule.getStart(), rule.getEnd(), rule, attributes);
    }
    
    /**
     * Constructs a new problem with the specified code, source location, rule
     * and attributes.
     * 
     * @param code Code of this problem.
     * @param start Start location of this problem in character stream.
     * @param end End location of this problem in character stream.
     * @param rule Rule that is the cause of this problem.
     * @param attributes Attributes that will be substituted in this problem
     *        description message.
     */
    Problem(Code code, Location start, Location end, Rule rule, String... attributes) {
        this.code = code;
        this.start = start;
        this.end = end;
        this.attributes = attributes;
        if ((this.rule = rule) != null) {
            rule.addProblem(this);
        }
    }
    
    /**
     * Returns type of this problem.
     * 
     * @return Type of this problem.
     */
    public Type getType() {
        return code.getType();
    }
    
    /**
     * Returns code of this problem.
     * 
     * @return Code of this problem.
     */
    public Code getCode() {
        return code;
    }
    
    /**
     * Returns start location of this problem in character stream.
     * 
     * @return Start location of this problem in character stream.
     */
    public Location getStart() {
        return start;
    }
    
    /**
     * Returns end location of this problem in character stream.
     * 
     * @return End location of this problem in character stream.
     */
    public Location getEnd() {
        return end;
    }
    
    /**
     * Returns rule that is the cause of this problem.
     * 
     * @return Rule that is the cause of this problem or <code>null</code> if
     *         this problem is not related to any grammar rule.
     */
    public Rule getRule() {
        return rule;
    }
    
    /**
     * Returns source of this problem if available.
     * 
     * @return Source of this problem or <code>null</code> if source is not
     *         available.
     */
    public String getSource() {
        if (rule == null || start == Location.UNKNOWN || end == Location.UNKNOWN) {
            return null;
        } else {
            return rule.getGrammar().getSource(start, end);
        }
    }
    
    /**
     * Returns description message of this problem.
     * 
     * @return Description message of this problem.
     */
    public String getMessage() {
        return ResourceManager.getProblemMessage(code, attributes);
    }
    
    /**
     * Compares this problem with another one for order.
     * 
     * @return A negative integer, zero, or a positive integer as this problem
     *         has less order, equal order, or greater order than the specified
     *         problem.
     */
    public int compareTo(Problem other) {
        int c = start.compareTo(other.start);
        return c == 0 ? code.ordinal() - other.code.ordinal() : c;
    }
    
    /**
     * Returns string representation of this problem.
     * 
     * @return String representation of this problem.
     * @see #toString(StringBuilder)
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString(buf);
        return buf.toString();
    }
    
    /**
     * Appends string representation of this problem to the specified buffer.
     * 
     * @param buf Buffer to append.
     */
    public void toString(StringBuilder buf) {
        getStart().toString(buf);
        buf.append("\n")
           .append(getType())
           .append(": ")
           .append(getMessage())
           .append("\n");
    }
    
    // Type
    
    /**
     * Enumeration of possible problem types.
     * 
     * @author Fox Mulder
     */
    public enum Type {
        
        FATAL, ERROR, WARNING, HINT
        
    }
    
    // Code
    
    /**
     * Enumeration of possible problem codes.
     * 
     * @author Fox Mulder
     */
    public enum Code {
        
        // Fatal errors
        
        /**
         * Grammar must define at least one production.
         */
        EMPTY_GRAMMAR(Type.FATAL),
        
        // Syntax errors
        
        /**
         * Syntax error.
         */
        SYNTAX_ERROR(Type.ERROR),
        
        /**
         * Expression is invalid.
         */
        INVALID_EXPRESSION(Type.ERROR),
        
        /**
         * The <code>;<code> character is missing.
         */
        MISSING_SEMI(Type.ERROR),
        
        /**
         * Closing parenthesize is missing.
         */
        MISSING_CLOSING_PARENTHESIZE(Type.ERROR),
        
        /**
         * String is not properly closed.
         */
        UNTERMINATED_STRING(Type.ERROR),
        
        /**
         * Escape sequence is invalid.
         */
        INVALID_ESCAPE_SEQUENCE(Type.ERROR),
        
        /**
         * Declaration of unicode character is invalid.
         */
        INVALID_UNICODE_CHARACTER(Type.ERROR),
        
        /**
         * Block comment is not properly closed.
         */
        UNTERMINATED_BLOCK_COMMENT(Type.ERROR),
        
        // Semantic errors
        
        /**
         * Left recursion detected.
         */
        LEFT_RECURSION(Type.ERROR),
        
        /**
         * Terminal does not match any characters.
         */
        EMPTY_TERMINAL(Type.ERROR),
        
        /**
         * Character class is not supported.
         */
        UNSUPPORTED_CLASS(Type.ERROR),
        
        /**
         * Production has been already declared.
         */
        DUPLICATE_PRODUCTION(Type.ERROR),
        
        /**
         * Reference to undefined production was found.
         */
        UNDEFINED_PRODUCTION(Type.ERROR),
        
        // Warnings
        
        /**
         * Production is never used.
         */
        UNUSED_PRODUCTION(Type.WARNING),
        
        /**
         * Action is not defined.
         */
        UNDEFINED_ACTION(Type.WARNING),
        
        // Hints
        
        /**
         * Terminal could be replaced by more efficient terminal.
         */
        INEFFICIENT_TERMINAL(Type.HINT),
        
        /**
         * Concatenation could be optimized.
         */
        INEFFICIENT_CONCATENATION(Type.HINT),
        
        /**
         * Alternation could be optimized.
         */
        INEFFICIENT_ALTERNATION(Type.HINT);
        
        /**
         * Type of the problem.
         */
        private final Type type;
        
        /**
         * Constructs a new problem code with the specified type.
         * 
         * @param type Type of the problem.
         */
        private Code(Type type) {
            this.type = type;
        }
        
        /**
         * Returns type of the problem.
         * 
         * @return Type of the problem.
         */
        public Type getType() {
            return type;
        }
        
    }
    
}
