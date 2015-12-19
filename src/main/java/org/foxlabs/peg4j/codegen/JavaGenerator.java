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

package org.foxlabs.peg4j.codegen;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;

import org.foxlabs.peg4j.Parser;
import org.foxlabs.peg4j.Transaction;
import org.foxlabs.peg4j.ActionContext;
import org.foxlabs.peg4j.ActionHandler;
import org.foxlabs.peg4j.grammar.*;
import org.foxlabs.peg4j.resource.ResourceManager;

import org.foxlabs.util.Location;
import org.foxlabs.util.UnicodeSet;

public final class JavaGenerator extends BaseGenerator {
    
    public static final int INCLUDE_DEBUGINFO = 0x01;
    public static final int GENERATE_COMMENTS = 0x02;
    public static final int GENERATE_PROBLEMS = 0x04;
    
    static final Set<Class<?>> DEFAULT_IMPORTS = new LinkedHashSet<Class<?>>();
    
    static {
        DEFAULT_IMPORTS.add(Grammar.class);
        DEFAULT_IMPORTS.add(GrammarBuilder.class);
        DEFAULT_IMPORTS.add(Parser.class);
        DEFAULT_IMPORTS.add(Transaction.class);
        DEFAULT_IMPORTS.add(ActionHandler.class);
        DEFAULT_IMPORTS.add(ActionContext.class);
    }
    
    static final class DeclContext {
        
        final Grammar grammar;
        final Map<String, String> variables;
        
        final Set<Class<?>> imports = new LinkedHashSet<Class<?>>(DEFAULT_IMPORTS);
        final Set<String> actions = new LinkedHashSet<String>();
        
        DeclContext(Grammar grammar, Map<String, String> variables) {
            this.grammar = grammar;
            this.variables = variables;
        }
        
    }
    
    private String className = null;
    private String namespace = null;
    private Class<?> result = null;
    private String actionPrefix = "handle";
    
    private boolean includeDebugInfo;
    private boolean generateComments;
    private boolean generateProblems;
    
    public JavaGenerator() {
        this(null, null, null, 0x07);
    }
    
    public JavaGenerator(int flags) {
        this(null, null, null, flags);
    }
    
    public JavaGenerator(String namespace, int flags) {
        this(null, namespace, null, flags);
    }
    
    public JavaGenerator(String name, String namespace, Class<?> result, int flags) {
        this.className = name == null ? "MyParser" : name;
        this.namespace = namespace == null ? "" : namespace;
        this.result = result == null ? Object.class : result;
        setFlags(flags);
    }
    
    public JavaGenerator(String name, String namespace, Class<?> result, String actionPrefix, int flags) {
        this.className = name == null ? "MyParser" : name;
        this.namespace = namespace == null ? "" : namespace;
        this.result = result == null ? Object.class : result;
        this.actionPrefix = actionPrefix == null || actionPrefix.trim().isEmpty() ? "handle" : actionPrefix.trim();
        setFlags(flags);
    }
    
    public int getFlags() {
        return (includeDebugInfo ? INCLUDE_DEBUGINFO : 0)
             | (generateComments ? GENERATE_COMMENTS : 0)
             | (generateProblems ? GENERATE_PROBLEMS : 0);
    }
    
    public void setFlags(int flags) {
        includeDebugInfo = (flags & INCLUDE_DEBUGINFO) != 0;
        generateComments = (flags & GENERATE_COMMENTS) != 0;
        generateProblems = (flags & GENERATE_PROBLEMS) != 0;
    }
    
    @Override
    protected String getTemplate() {
        return ResourceManager.getGrammarJavaTemplate();
    }
    
    @Override
    protected void defineVariables(Grammar grammar, Map<String, String> variables) {
        super.defineVariables(grammar, variables);
        DeclContext dc = new DeclContext(grammar, variables);
        definePackageDecl(dc);
        defineParserType(dc);
        defineResultType(dc);
        defineImportDecls(dc);
        defineGrammarStatements(dc);
        defineActionMethods(dc);
        defineProblems(dc);
    }
    
    // ${package_decl}
    private void definePackageDecl(DeclContext dc) {
        String packageDecl = "package " + namespace + ";";
        dc.variables.put("package_decl", packageDecl);
    }
    
    // ${parser_type}
    private void defineParserType(DeclContext dc) {
        dc.variables.put("parser_type", className);
    }
    
    // ${result_type}
    private void defineResultType(DeclContext dc) {
        if (!Object.class.getPackage().getName().equals(result.getPackage().getName())) {
            dc.imports.add(result);
        }
        dc.variables.put("result_type", result.getSimpleName());
    }
    
    // ${import_decls}
    private void defineImportDecls(DeclContext dc) {
        Set<String> imports = new TreeSet<String>();
        for (Class<?> type : dc.imports) {
            Package pkg = type.getPackage();
            if (!Object.class.getPackage().equals(pkg)) {
                StringBuilder buf = new StringBuilder();
                if (pkg != null) {
                    buf.append(pkg.getName());
                    buf.append(".");
                }
                appendTypeDecl(type, buf);
                imports.add(buf.toString());
            }
        }
        StringBuilder importDecls = new StringBuilder();
        for (String name : imports) {
            importDecls.append("import ");
            importDecls.append(name);
            importDecls.append(";\n");
        }
        dc.variables.put("import_decls", importDecls.toString());
    }
    
    // ${grammar_statements}
    private void defineGrammarStatements(DeclContext dc) {
        RuleWriter rw = new RuleWriter(dc);
        int count = dc.grammar.getProductionCount();
        for (int i = 0; i < count; i++) {
            dc.grammar.getProduction(i).accept(rw);
        }
        dc.variables.put("grammar_statements", rw.statements.toString());
    }
    
    // ${action_methods}
    private void defineActionMethods(DeclContext dc) {
        StringBuilder actionMethods = new StringBuilder();
        for (String action : dc.actions) {
            actionMethods.append("private boolean ");
            actionMethods.append(action);
            actionMethods.append("(");
            actionMethods.append(ActionContext.class.getSimpleName());
            actionMethods.append(" context) throws ");
            actionMethods.append(Throwable.class.getSimpleName());
            actionMethods.append(" {\n");
            appendIdent(3, actionMethods);
            actionMethods.append("// TODO\n");
            appendIdent(3, actionMethods);
            actionMethods.append("return true;\n");
            actionMethods.append("}\n\n");
        }
        dc.variables.put("action_methods", actionMethods.toString());
    }
    
    // ${problems}
    private void defineProblems(DeclContext dc) {
        StringBuilder problemComments = new StringBuilder();
        if (generateComments && generateProblems && dc.grammar.hasProblems()) {
            appendComment("PROBLEMS:", problemComments);
            problemComments.append("\n");
            for (Problem problem : dc.grammar.getProblems().getProblems()) {
                if (Location.UNKNOWN != problem.getStart()) {
                    appendComment(problem.toString(), problemComments);
                    problemComments.append("\n");
                }
            }
        }
        dc.variables.put("problems", problemComments.toString());
    }
    
    // RuleWriter
    
    final class RuleWriter implements RuleVisitor<RuntimeException> {
        
        final DeclContext dc;
        final StringBuilder statements = new StringBuilder();
        
        RuleWriter(DeclContext dc) {
            this.dc = dc;
        }
        
        public void visit(Terminal rule) {
            if (rule instanceof Terminal.Token) {
                Terminal.Token term = (Terminal.Token) rule;
                
                if (term.isCaseSensitive()) {
                    statements.append(".pushTokenCS(");
                } else {
                    statements.append(".pushTokenIC(");
                }
                statements.append("\"");
                statements.append(UnicodeSet.escape(term.getImage()));
                statements.append("\")\n");
                
                appendStartLocation(rule);
                appendEndLocation(rule);
            } else if (rule instanceof Terminal.Interval) {
                Terminal.Interval term = (Terminal.Interval) rule;
                
                statements.append(".pushInterval(");
                statements.append("\'");
                statements.append(UnicodeSet.escape((char) term.getMin()));
                statements.append("\', \'");
                statements.append(UnicodeSet.escape((char) term.getMax()));
                statements.append("\')\n");
                
                appendStartLocation(rule);
                appendEndLocation(rule);
            } else if (rule instanceof Terminal.Set) {
                Terminal.Set term = (Terminal.Set) rule;
                
                int[] intervals = term.getUnicodeSet().toArray();
                statements.append(".pushSet(");
                if (term.getUnicodeSet().isElementSet()) {
                    statements.append("\'");
                    statements.append(UnicodeSet.escape((char) intervals[0]));
                    statements.append("\'");
                    for (int i = 2; i < intervals.length; i += 2) {
                        statements.append(", \'");
                        statements.append(UnicodeSet.escape((char) intervals[i]));
                        statements.append("\'");
                    }
                    statements.append(")\n");
                } else {
                    statements.append(UnicodeSet.class.getSimpleName());
                    statements.append(".fromIntervals(new int[]{\'");
                    statements.append(UnicodeSet.escape((char) intervals[0]));
                    statements.append("\'");
                    for (int i = 1; i < intervals.length; i++) {
                        statements.append(", \'");
                        statements.append(UnicodeSet.escape((char) intervals[i]));
                        statements.append("\'");
                    }
                    statements.append("}))\n");
                    dc.imports.add(UnicodeSet.class);
                }
                
                appendStartLocation(rule);
                appendEndLocation(rule);
            } else if (rule instanceof Terminal.Class) {
                Terminal.Class term = (Terminal.Class) rule;
                
                statements.append(".pushClass(");
                statements.append("\"");
                statements.append(UnicodeSet.escape(term.getName()));
                statements.append("\")\n");
                
                appendStartLocation(rule);
                appendEndLocation(rule);
            } else if (rule instanceof Terminal.Any) {
                statements.append(".pushAny()\n");
                
                appendStartLocation(rule);
                appendEndLocation(rule);
            }
        }
        
        public void visit(Production rule) {
            if (statements.length() > 0) {
                statements.append("\n");
            }
            
            if (generateComments) {
                appendComment(rule.toString(), statements);
                if (generateProblems) {
                    for (Problem problem : rule.getAllProblems()) {
                        appendComment(problem.toString(), statements);
                    }
                }
            }
            
            statements.append(".startProduction(");
            statements.append("\"");
            statements.append(UnicodeSet.escape(rule.getName()));
            statements.append("\"");
            
            if (includeDebugInfo) {
                Location start = rule.getStart();
                if (Location.UNKNOWN != start) {
                    statements.append(", ");
                    appendLocation(start, statements);
                }
            }
            
            statements.append(")\n");
            
            rule.getExpression().accept(this);
            
            statements.append(".endProduction(");
            
            if (includeDebugInfo) {
                Location end = rule.getEnd();
                if (Location.UNKNOWN != end) {
                    appendLocation(end, statements);
                }
            }
            
            statements.append(")\n");
        }
        
        public void visit(Reference rule) {
            statements.append(".pushReference(");
            
            statements.append("\"");
            statements.append(UnicodeSet.escape(rule.getTargetName()));
            statements.append("\"");
            
            Modifier mod = rule.getModifier();
            if (mod != null) {
                statements.append(", ");
                statements.append(Modifier.class.getSimpleName());
                statements.append(".");
                statements.append(mod.name());
                dc.imports.add(Modifier.class);
            }
            
            statements.append(")\n");
            
            appendStartLocation(rule);
            appendEndLocation(rule);
        }
        
        public void visit(Action rule) {
            rule.getChild().accept(this);
            
            if (!rule.isInjected()) {
                if (rule.getChild() instanceof Terminal.Nil) {
                    statements.append(".mark()\n");
                }
                
                String name = rule.getName();
                name = actionPrefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                dc.actions.add(name);
                
                statements.append(".action(");
                statements.append("\"");
                statements.append(rule.getName());
                statements.append("\", new ");
                statements.append(ActionHandler.class.getSimpleName());
                statements.append("<");
                statements.append(JavaGenerator.this.className);
                statements.append(">() {\n");
                appendIdent(3, statements);
                statements.append("public boolean handle(");
                statements.append(JavaGenerator.this.className);
                statements.append(" parser, ");
                statements.append(ActionContext.class.getSimpleName());
                statements.append(" context) throws ");
                statements.append(Throwable.class.getSimpleName());
                statements.append(" {\n");
                appendIdent(7, statements);
                statements.append("return parser.");
                statements.append(name);
                statements.append("(context);\n");
                appendIdent(3, statements);
                statements.append("}\n");
                statements.append("})\n");
                
                if (rule.getChild() instanceof Terminal.Nil) {
                    statements.append(".release()\n");
                }
                
                appendStartLocation(rule);
                appendEndLocation(rule);
            }
        }
        
        public void visit(Concatenation rule) {
            statements.append(".mark()\n");
            
            int length = rule.length();
            for (int i = 0; i < length; i++) {
                rule.getChild(i).accept(this);
            }
            
            statements.append(".concat(");
            statements.append(").release()\n");
            
            appendStartLocation(rule);
            appendEndLocation(rule);
        }
        
        public void visit(Alternation rule) {
            statements.append(".mark()\n");
            
            int length = rule.length();
            for (int i = 0; i < length; i++) {
                rule.getChild(i).accept(this);
            }
            
            statements.append(".choice(");
            statements.append(").release()\n");
            
            appendStartLocation(rule);
            appendEndLocation(rule);
        }
        
        public void visit(Repetition rule) {
            rule.getChild().accept(this);
            
            Quantifier quant = rule.getQuantifier();
            statements.append(".repeat(");
            if (quant == null) {
                int min = rule.getMin();
                int max = rule.getMax();
                statements.append(min);
                if (min < max) {
                    statements.append(", ");
                    if (max == Integer.MAX_VALUE) {
                        statements.append(Integer.class.getSimpleName());
                        statements.append(".");
                        statements.append("MAX_VALUE");
                    } else {
                        statements.append(max);
                    }
                }
            } else {
                statements.append(Quantifier.class.getSimpleName());
                statements.append(".");
                statements.append(rule.getQuantifier().name());
            }
            statements.append(")\n");
            
            appendStartLocation(rule);
            appendEndLocation(rule);
            
            dc.imports.add(Quantifier.class);
        }
        
        public void visit(Exclusion rule) {
            rule.getChild().accept(this);
            
            statements.append(".except(");
            statements.append(Predicate.class.getSimpleName());
            statements.append(".");
            statements.append(rule.getPredicate().name());
            statements.append(")\n");
            
            appendStartLocation(rule);
            appendEndLocation(rule);
            
            dc.imports.add(Predicate.class);
        }
        
        private void appendStartLocation(Rule rule) {
            if (includeDebugInfo) {
                Location start = rule.getStart();
                if (Location.UNKNOWN != start) {
                    statements.append(".setStart(");
                    appendLocation(start, statements);
                    statements.append(")\n");
                    dc.imports.add(Location.class);
                }
            }
        }
        
        private void appendEndLocation(Rule rule) {
            if (includeDebugInfo) {
                Location end = rule.getEnd();
                if (Location.UNKNOWN != end) {
                    appendIdent(7, statements);
                    statements.append(".setEnd(");
                    appendLocation(end, statements);
                    statements.append(")\n");
                    dc.imports.add(Location.class);
                }
            }
        }
        
    }
    
    // Utils
    
    static void appendComment(String comment, StringBuilder buf) {
        StringTokenizer tokenizer = new StringTokenizer(comment, "\n");
        while (tokenizer.hasMoreTokens()) {
            buf.append("// ");
            buf.append(tokenizer.nextToken());
            buf.append("\n");
        }
    }
    
    static void appendLocation(Location location, StringBuilder buf) {
        buf.append(Location.class.getSimpleName());
        buf.append(".valueOf(");
        buf.append(location.line);
        buf.append(", ");
        buf.append(location.column);
        buf.append(")");
    }
    
    static void appendTypeDecl(Type type, DeclContext dc, StringBuilder buf) {
        if (type instanceof TypeVariable<?>) {
            appendTypeDecl((TypeVariable<?>) type, dc, buf);
        } else if (type instanceof ParameterizedType) {
            appendTypeDecl((ParameterizedType) type, dc, buf);
        } else if (type instanceof WildcardType) {
            appendTypeDecl((WildcardType) type, dc, buf);
        } else if (type instanceof GenericArrayType) {
            appendTypeDecl((GenericArrayType) type, dc, buf);
        } else {
            appendTypeDecl((Class<?>) type, dc, buf);
        }
    }
    
    static void appendTypeDecl(Class<?> type, StringBuilder buf) {
        Class<?> owner = type.getEnclosingClass();
        if (owner != null) {
            appendTypeDecl(owner, buf);
            buf.append(".");
        }
        buf.append(type.getSimpleName());
    }
    
    static void appendTypeDecl(Class<?> type, DeclContext dc, StringBuilder buf) {
        buf.append(type.getSimpleName());
        dc.imports.add(type);
    }
    
    static void appendTypeDecl(TypeVariable<?> type, DeclContext dc, StringBuilder buf) {
        buf.append(type.getName());
        Type[] bounds = type.getBounds();
        if (bounds.length > 0) {
            buf.append(" extends ");
            appendTypeDecl(bounds[0], dc, buf);
            for (int i = 1; i < bounds.length; i++) {
                buf.append(" & ");
                appendTypeDecl(bounds[i], dc, buf);
            }
        }
    }
    
    static void appendTypeDecl(ParameterizedType type, DeclContext dc, StringBuilder buf) {
        appendTypeDecl(type.getRawType(), dc, buf);
        Type[] args = type.getActualTypeArguments();
        if (args.length > 0) {
            buf.append("<");
            appendTypeDecl(args[0], dc, buf);
            for (int i = 1; i < args.length; i++) {
                buf.append(", ");
                appendTypeDecl(args[i], dc, buf);
            }
            buf.append(">");
        }
    }
    
    static void appendTypeDecl(WildcardType type, DeclContext dc, StringBuilder buf) {
        buf.append("?");
        Type[] bounds = type.getLowerBounds();
        if (!(bounds.length == 0 || bounds[0] == Object.class)) {
            buf.append(" super ");
            appendTypeDecl(bounds[0], dc, buf);
            for (int i = 1; i < bounds.length; i++) {
                buf.append(" & ");
                appendTypeDecl(bounds[i], dc, buf);
            }
        } else {
            bounds = type.getUpperBounds();
            if (!(bounds.length == 0 || bounds[0] == Object.class)) {
                buf.append(" extends ");
                appendTypeDecl(bounds[0], dc, buf);
                for (int i = 1; i < bounds.length; i++) {
                    buf.append(" & ");
                    appendTypeDecl(bounds[i], dc, buf);
                }
            }
        }
    }
    
    static void appendTypeDecl(GenericArrayType type, DeclContext dc, StringBuilder buf) {
        appendTypeDecl(type.getGenericComponentType(), dc, buf);
        buf.append("[]");
    }
    
}
