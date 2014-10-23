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

public final class Problem implements Serializable, Comparable<Problem> {
    private static final long serialVersionUID = -7144206945196894506L;
    
    public enum Kind { FATAL, ERROR, WARNING, HINT }
    
    private static final int fCodeBase = 1000;
    private static final int eCodeBase = 2000;
    private static final int wCodeBase = 3000;
    private static final int hCodeBase = 4000;
    
    static final String[] emptyAttributes = new String[0];
    
    final int code;
    final Location start;
    final Location end;
    final String[] attributes;
    
    transient Rule source;
    
    Problem(int code, Location start, Location end) {
        this(code, start, end, null, emptyAttributes);
    }
    
    Problem(int code, Location start, Location end, String... attributes) {
        this(code, start, end, null, attributes);
    }
    
    Problem(int code, Rule source) {
        this(code, source.getStart(), source.getEnd(), source, emptyAttributes);
    }
    
    Problem(int code, Rule source, String... attributes) {
        this(code, source.getStart(), source.getEnd(), source, attributes);
    }
    
    Problem(int code, Location start, Location end, Rule source, String... attributes) {
        this.code = code;
        this.start = start;
        this.end = end;
        this.attributes = attributes;
        if ((this.source = source) != null)
            source.addProblem(this);
    }
    
    public int getCode() {
        return code;
    }
    
    public Location getStart() {
        return start;
    }
    
    public Location getEnd() {
        return end;
    }
    
    public Rule getSource() {
        return source;
    }
    
    public Kind getKind() {
        return code < eCodeBase
            ? Kind.FATAL
            : code < wCodeBase
                ? Kind.ERROR
                : code < hCodeBase
                    ? Kind.WARNING
                    : Kind.HINT;
    }
    
    public String getMessage() {
        int index;
        String[] messages;
        
        if (code < eCodeBase) {
            index = code - fCodeBase;
            messages = fatals;
        } else if (code < wCodeBase) {
            index = code - eCodeBase;
            messages = errors;
        } else if (code < hCodeBase) {
            index = code - wCodeBase;
            messages = warnings;
        } else {
            index = code - hCodeBase;
            messages = hints;
        }
        
        return ResourceManager.getMessage(messages[index], (Object[]) attributes);
    }
    
    public int compareTo(Problem other) {
        int c = start.compareTo(other.start);
        return c == 0 ? code - other.code : c;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString(buf);
        return buf.toString();
    }
    
    public void toString(StringBuilder buf) {
        getStart().toString(buf);
        buf.append("\n")
           .append(getKind())
           .append(": ")
           .append(getMessage())
           .append("\n");
    }
    
    // Fatal messages
    
    public static final int fatalEmptyGrammar               = fCodeBase + 0;
    
    private static final String[] fatals = new String[]{
        "fatal.emptyGrammar"
    };
    
    // Error messages
    
    // Syntax error codes
    public static final int errorSyntax                     = eCodeBase + 0;
    public static final int errorInvalidExpression          = eCodeBase + 1;
    public static final int errorMissingSemi                = eCodeBase + 2;
    public static final int errorMissingClosingParenthesize = eCodeBase + 3;
    public static final int errorUnterminatedString         = eCodeBase + 4;
    public static final int errorInvalidEscapeSequence      = eCodeBase + 5;
    public static final int errorInvalidUnicodeCharacter    = eCodeBase + 6;
    public static final int errorUnterminatedBlockComment   = eCodeBase + 7;
    // Semantic error codes
    public static final int errorLeftRecursion              = eCodeBase + 8;
    public static final int errorUnsafeSkipReference        = eCodeBase + 9;
    public static final int errorEmptyTerminal              = eCodeBase + 10;
    public static final int errorUnsupportedClass           = eCodeBase + 11;
    public static final int errorDuplicateProduction        = eCodeBase + 12;
    public static final int errorUndefinedProduction        = eCodeBase + 13;
    
    private static final String[] errors = new String[]{
        // Syntax error messages
        "error.syntaxError",
        "error.invalidExpression",
        "error.missingSemi",
        "error.missingClosingParenthesize",
        "error.unterminatedString",
        "error.invalidEscapeSequence",
        "error.invalidUnicodeCharacter",
        "error.unterminatedBlockComment",
        // Semantic error messages
        "error.leftRecursion",
        "error.unsafeSkipReference",
        "error.emptyTerminal",
        "error.unsupportedClass",
        "error.duplicateProduction",
        "error.undefinedProduction"
    };
    
    // Warning messages
    
    public static final int warningUnusedProduction         = wCodeBase + 0;
    public static final int warningUndefinedAction          = wCodeBase + 1;
    
    private static final String[] warnings = new String[]{
        "warning.unusedProduction",
        "warning.undefinedAction"
    };
    
    // Hint messages
    
    public static final int hintInefficientTerminal         = hCodeBase + 0;
    public static final int hintInefficientConcatenation    = hCodeBase + 1;
    public static final int hintInefficientAlternation      = hCodeBase + 2;
    public static final int hintPossibleSkipReference       = hCodeBase + 3;
    
    private static final String[] hints = new String[]{
        "hint.inefficientTerminal",
        "hint.inefficientConcatenation",
        "hint.inefficientAlternation",
        "hint.possibleSkipReference"
    };
    
}
