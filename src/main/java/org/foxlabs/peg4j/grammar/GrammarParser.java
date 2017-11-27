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

import org.foxlabs.peg4j.Parser;
import org.foxlabs.peg4j.ActionHandler;
import org.foxlabs.peg4j.ActionContext;
import org.foxlabs.peg4j.Transaction;
import org.foxlabs.peg4j.LocalStack;

import org.foxlabs.util.Location;
import org.foxlabs.util.Strings;
import org.foxlabs.util.UnicodeSet;

import static org.foxlabs.peg4j.grammar.Problem.Code.*;

public final class GrammarParser extends Parser<Grammar> {
    
    private final GrammarBuilder builder;
    
    private final LocalStack<String> symbolStack = new LocalStack<String>();
    private final LocalStack<Integer> intStack = new LocalStack<Integer>();
    private final LocalStack<UnicodeSet> usetStack = new LocalStack<UnicodeSet>();
    private final LocalStack<Problem> problemStack = new LocalStack<Problem>();
    
    private final Tx transaction = new Tx();
    
    private String source = null;
    private Location eofLocation = null;
    
    private int compilationFlags = 0;
    
    public GrammarParser() {
        this(new GrammarBuilder());
    }
    
    public GrammarParser(Map<String, ActionHandler<?>> handlers) {
        this(new GrammarBuilder(handlers));
    }
    
    public GrammarParser(GrammarBuilder builder) {
        this.builder = builder;
    }
    
    public void setCompilationFlags(int flags) {
        compilationFlags = flags;
    }
    
    @Override
    public Grammar getGrammar() {
        return GRAMMAR;
    }
    
    @Override
    protected Transaction getTransaction() {
        return transaction;
    }
    
    @Override
    protected Grammar buildResult() {
        GrammarProblems problems = new GrammarProblems();
        problems.addAll(problemStack.popAll(new Problem[problemStack.size()]));
        Grammar grammar = builder.buildGrammar(problems, source);
        GrammarCompiler.compile(grammar, compilationFlags);
        
        if (grammar.getProductionCount() == 0) {
            problems.add(EMPTY_GRAMMAR, eofLocation, eofLocation);
            problems.sort();
        }
        
        return grammar;
    }
    
    public void reset() {
        builder.clear();
        symbolStack.clear();
        intStack.clear();
        usetStack.clear();
        problemStack.clear();
        source = null;
        eofLocation = null;
    }
    
    // Transaction

    private final class Tx implements Transaction {
        
        private Expression[] rules;
        private String[] symbols;
        private Integer[] ints;
        private UnicodeSet[] usets;
        private Problem[] problems;
        
        @Override
        public void begin() {
            builder.mark();
            symbolStack.mark();
            intStack.mark();
            usetStack.mark();
            problemStack.mark();
        }
        
        @Override
        public void commit() {
            builder.release();
            symbolStack.release();
            intStack.release();
            usetStack.release();
            problemStack.release();
        }
        
        @Override
        public void rollback() {
            builder.reset();
            symbolStack.reset();
            intStack.reset();
            usetStack.reset();
            problemStack.reset();
        }
        
        @Override
        public boolean load() {
            builder.pushAll(rules);
            symbolStack.pushAll(symbols);
            intStack.pushAll(ints);
            usetStack.pushAll(usets);
            problemStack.pushAll(problems);
            return true;
        }
        
        @Override
        public Tx save() {
            Tx tx = new Tx();
            tx.rules = builder.peekAll(new Expression[builder.size()]);
            tx.symbols = symbolStack.peekAll(new String[symbolStack.size()]);
            tx.ints = intStack.peekAll(new Integer[intStack.size()]);
            tx.usets = usetStack.peekAll(new UnicodeSet[usetStack.size()]);
            tx.problems = problemStack.peekAll(new Problem[problemStack.size()]);
            return tx;
        }
        
    }
    
    // Actions
    
    private boolean handleEof(ActionContext context) {
        eofLocation = context.start();
        return true;
    }
    
    private boolean handleSource(ActionContext context) {
        this.source = context.text();
        return true;
    }
    
    private boolean handleSymbol(ActionContext context) {
        symbolStack.push(context.text());
        return true;
    }
    
    private boolean handleDecode(ActionContext context) {
        symbolStack.push(Strings.unescape(context.text()));
        return true;
    }
    
    private boolean handleInt(ActionContext context) {
        intStack.push(Integer.valueOf(context.text()));
        return true;
    }
    
    private boolean handleUnlimited() {
        intStack.push(Integer.MAX_VALUE);
        return true;
    }
    
    private boolean handleProductionStart(ActionContext context) {
        String name = symbolStack.pop();
        builder.startProduction(name, context.start());
        return true;
    }
    
    private boolean handleProductionEnd(ActionContext context) {
        builder.endProduction(context.end());
        return true;
    }
    
    private boolean handleRuleChoice(ActionContext context) {
        builder.choice().setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleRuleConcat(ActionContext context) {
        builder.concat().setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleRuleRepeat(ActionContext context) {
        if (symbolStack.size() > 0) {
            Quantifier quant = Quantifier.quantifierOf(symbolStack.pop());
            builder.repeat(quant).setStart(context.start()).setEnd(context.end());
        } else if (intStack.size() == 1) {
            builder.repeat(intStack.pop()).setStart(context.start()).setEnd(context.end());
        } else if (intStack.size() == 2) {
            int max = intStack.pop();
            int min = intStack.pop();
            builder.repeat(min, max).setStart(context.start()).setEnd(context.end());
        }
        return true;
    }
    
    private boolean handleRuleExcept(ActionContext context) {
        if (symbolStack.size() > 0) {
            Predicate pred = Predicate.predicateOf(symbolStack.pop());
            builder.except(pred).setStart(context.start()).setEnd(context.end());
        }
        return true;
    }
    
    private boolean handleRuleReference(ActionContext context) {
        String name = symbolStack.pop();
        Modifier mod = symbolStack.size() > 0 ? Modifier.modifierOf(symbolStack.pop()) : null;
        builder.pushReference(name, mod).setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleRuleAction(ActionContext context) {
        String name = symbolStack.pop();
        builder.action(name).setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleTerminalInterval(ActionContext context) {
        char max = symbolStack.pop().charAt(0);
        char min = symbolStack.pop().charAt(0);
        builder.pushInterval(min, max).setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleTerminalTokenCS(ActionContext context) {
        builder.pushToken(symbolStack.pop(), true).setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleTerminalTokenIC(ActionContext context) {
        builder.pushToken(symbolStack.pop(), false).setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleTerminalSet(ActionContext context) {
        builder.pushSet(usetStack.pop()).setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleTerminalClass(ActionContext context) {
        builder.pushClass(symbolStack.pop()).setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleTerminalAny(ActionContext context) {
        builder.pushAny().setStart(context.start()).setEnd(context.end());
        return true;
    }
    
    private boolean handleSetInverse() {
        usetStack.push(usetStack.pop().inverse());
        return true;
    }
    
    private boolean handleSetInterval() {
        char max = symbolStack.pop().charAt(0);
        char min = symbolStack.pop().charAt(0);
        usetStack.push(UnicodeSet.fromIntervals(min, max));
        return true;
    }
    
    private boolean handleSetElement() {
        char ch = symbolStack.pop().charAt(0);
        usetStack.push(UnicodeSet.fromElements(ch));
        return true;
    }
    
    private boolean handleSetUnion() {
        usetStack.push(UnicodeSet.unionAll(usetStack.popAll(new UnicodeSet[usetStack.size()])));
        return true;
    }
    
    private boolean handleInvalidExpression(ActionContext context) {
        problemStack.push(new Problem(INVALID_EXPRESSION, context.start(), context.end()));
        return true;
    }
    
    private boolean handleMissingSemi(ActionContext context) {
        problemStack.push(new Problem(MISSING_SEMI, context.start(), context.end()));
        return true;
    }
    
    private boolean handleErrorStart(ActionContext context) {
        return true;
    }
    
    private boolean handleEndError(ActionContext context) {
        problemStack.push(new Problem(SYNTAX_ERROR, context.start(), context.end()));
        return true;
    }
    
    private boolean handleErrorMissingClosingParenthesize(ActionContext context) {
        problemStack.push(new Problem(MISSING_CLOSING_PARENTHESIZE, context.start(), context.end()));
        return true;
    }
    
    private boolean handleErrorUnterminatedString(ActionContext context) {
        problemStack.push(new Problem(UNTERMINATED_STRING, context.start(), context.end()));
        return true;
    }
    
    private boolean handleErrorInvalidEscapeSequence(ActionContext context) {
        problemStack.push(new Problem(INVALID_ESCAPE_SEQUENCE, context.start(), context.end()));
        return true;
    }
    
    private boolean handleErrorInvalidUnicodeCharacter(ActionContext context) {
        problemStack.push(new Problem(INVALID_UNICODE_CHARACTER, context.start(), context.end()));
        return true;
    }
    
    private boolean handleErrorUnterminatedBlockComment(ActionContext context) {
        problemStack.push(new Problem(UNTERMINATED_BLOCK_COMMENT, context.start(), context.end()));
        return true;
    }
    
    // Grammar
    
    private static final Grammar GRAMMAR = new GrammarBuilder()
            .startProduction("Grammar")
            .mark()
            .mark()
            .pushReference("Production", Modifier.MEMO)
            .mark()
            .pushReference("Spacing")
            .pushReference("Garbage")
            .action("startError", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorStart(context);
                }
            })
            .mark()
            .pushReference("Production", Modifier.MEMO)
            .except(Predicate.NOT)
            .pushReference("Garbage")
            .concat().release()
            .repeat(Quantifier.ZEROORMORE)
            .mark()
            .action("endError", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleEndError(context);
                }
            })
            .release()
            .concat().release()
            .choice().release()
            .repeat(Quantifier.ZEROORMORE)
            .pushReference("Spacing")
            .pushAny()
            .except(Predicate.NOT)
            .action("eof", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleEof(context);
                }
            })
            .concat().release()
            .action("source", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSource(context);
                }
            })
            .endProduction()
            
            .startProduction("Garbage")
            .mark()
            .pushReference("Spacing")
            .mark()
            .pushReference("String")
            .repeat(Quantifier.ONCEORMORE)
            .pushAny()
            .choice().release()
            .concat().release()
            .endProduction()
            
            .startProduction("Production")
            .mark()
            .pushReference("Spacing")
            .mark()
            .pushReference("Identifier")
            .mark()
            .pushTokenCS(":")
            .pushTokenCS("=")
            .pushTokenCS("<-")
            .pushTokenCS("::=")
            .choice().release()
            .concat().release()
            .action("startProduction", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleProductionStart(context);
                }
            })
            .pushReference("Spacing")
            .mark()
            .mark()
            .pushReference("Expression", Modifier.MEMO)
            .pushTokenCS(";")
            .concat().release()
            .mark()
            .mark()
            .mark()
            .mark()
            .pushReference("Expression", Modifier.MEMO)
            .pushReference("Comment", Modifier.MEMO)
            .pushReference("Space", Modifier.MEMO)
            .choice().release()
            .except(Predicate.NOT)
            .pushSet(UnicodeSet.fromIntervals(new int[]{'\u0000', ':', '<', '\uffff'}))
            .concat().release()
            .repeat(Quantifier.ONCEORMORE)
            .action("invalidExpression", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleInvalidExpression(context);
                }
            })
            .pushReference("Expression", Modifier.MEMO)
            .pushReference("Comment", Modifier.MEMO)
            .pushReference("Space", Modifier.MEMO)
            .choice().release()
            .repeat(Quantifier.ZEROORMORE)
            .mark()
            .pushTokenCS(";")
            .mark()
            .action("missingSemi", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleMissingSemi(context);
                }
            })
            .release()
            .choice().release()
            .concat().release()
            .choice().release()
            .action("endProduction", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleProductionEnd(context);
                }
            })
            .concat().release()
            .endProduction()
            
            .startProduction("Expression")
            .pushReference("AlternationExpression")
            .endProduction()
            
            .startProduction("AlternationExpression")
            .mark()
            .pushReference("ConcatenationExpression")
            .mark()
            .pushSet('/', '|')
            .pushReference("Spacing")
            .pushReference("ConcatenationExpression")
            .concat().release()
            .repeat(Quantifier.ZEROORMORE)
            .concat().release()
            .action("ruleChoice", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleRuleChoice(context);
                }
            })
            .endProduction()
            
            .startProduction("ConcatenationExpression")
            .pushReference("RepetitionExpression")
            .repeat(Quantifier.ONCEORMORE)
            .action("ruleConcat", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleRuleConcat(context);
                }
            })
            .endProduction()
            
            .startProduction("RepetitionExpression")
            .mark()
            .pushReference("ExclusionExpression")
            .mark()
            .mark()
            .pushSet(UnicodeSet.fromIntervals(new int[]{'*', '+', '?', '?'}))
            .action("symbol", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSymbol(context);
                }
            })
            .mark()
            .pushTokenCS('{')
            .pushReference("Spacing")
            .pushReference("Integer")
            .mark()
            .pushTokenCS(',')
            .pushReference("Spacing")
            .mark()
            .pushReference("Integer")
            .mark()
            .action("unlimited", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleUnlimited();
                }
            })
            .release()
            .choice().release()
            .concat().release()
            .repeat(Quantifier.ONCEORNONE)
            .pushTokenCS('}')
            .concat().release()
            .choice().release()
            .pushReference("Spacing")
            .concat().release()
            .repeat(Quantifier.ONCEORNONE)
            .concat().release()
            .action("ruleRepeat", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleRuleRepeat(context);
                }
            })
            .endProduction()
            
            .startProduction("ExclusionExpression")
            .mark()
            .mark()
            .mark()
            .pushTokenCS(Predicate.NOT.toString())
            .pushTokenCS(Predicate.AND.toString())
            .choice().release()
            .action("symbol", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSymbol(context);
                }
            })
            .pushReference("Spacing")
            .concat().release()
            .repeat(Quantifier.ONCEORNONE)
            .pushReference("OperandExpression")
            .concat().release()
            .action("ruleExcept", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleRuleExcept(context);
                }
            })
            .endProduction()
            
            .startProduction("OperandExpression")
            .mark()
            .pushReference("TerminalExpression")
            .pushReference("ReferenceExpression")
            .pushReference("GroupingExpression")
            .pushReference("ActionExpression")
            .choice().release()
            .endProduction()
            
            .startProduction("ReferenceExpression")
            .mark()
            .mark()
            .pushTokenCS(Modifier.MEMO.toString())
            .choice().release()
            .action("symbol", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSymbol(context);
                }
            })
            .repeat(Quantifier.ONCEORNONE)
            .pushReference("Identifier")
            .concat().release()
            .action("ruleReference", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleRuleReference(context);
                }
            })
            .endProduction()
            
            .startProduction("GroupingExpression")
            .mark()
            .pushTokenCS("(")
            .pushReference("Spacing")
            .pushReference("Expression")
            .mark()
            .pushTokenCS(")")
            .mark()
            .action("missingClosingParenthesize", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorMissingClosingParenthesize(context);
                }
            })
            .release()
            .choice().release()
            .concat().release()
            .pushReference("Spacing")
            .endProduction()
            
            .startProduction("ActionExpression")
            .mark()
            .pushTokenCS("$")
            .pushReference("Identifier")
            .pushTokenCS("(")
            .pushReference("Spacing")
            .pushReference("Expression")
            .repeat(Quantifier.ONCEORNONE)
            .pushTokenCS(")")
            .concat().release()
            .action("ruleAction", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleRuleAction(context);
                }
            })
            .pushReference("Spacing")
            .endProduction()

            .startProduction("TerminalExpression")
            .mark()
            .pushReference("IntervalExpression")
            .pushReference("TokenExpression")
            .pushReference("SetExpression")
            .pushReference("ClassExpression")
            .pushReference("AnyExpression")
            .choice().release()
            .endProduction()
            
            .startProduction("IntervalExpression")
            .pushReference("IntervalElement")
            .action("terminalInterval", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleTerminalInterval(context);
                }
            })
            .endProduction()
            
            .startProduction("TokenExpression")
            .mark()
            .mark()
            .mark()
            .pushTokenCS("\'")
            .pushReference("Character")
            .repeat(Quantifier.ONCEORMORE)
            .action("decode", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleDecode(context);
                }
            })
            .mark()
            .pushTokenCS("\'")
            .mark()
            .mark()
            .pushTokenCS("\n")
            .pushAny()
            .except(Predicate.NOT)
            .choice().release()
            .mark()
            .action("unterminatedString", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorUnterminatedString(context);
                }
            })
            .release()
            .concat().release()
            .choice().release()
            .concat().release()
            .action("terminalTokenCS", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleTerminalTokenCS(context);
                }
            })
            .pushReference("Spacing")
            .concat().release()
            .mark()
            .mark()
            .pushTokenCS("\"")
            .pushReference("Character")
            .repeat(Quantifier.ONCEORMORE)
            .action("decode", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleDecode(context);
                }
            })
            .mark()
            .pushTokenCS("\"")
            .mark()
            .mark()
            .pushTokenCS("\n")
            .pushAny()
            .except(Predicate.NOT)
            .choice().release()
            .mark()
            .action("unterminatedString", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorUnterminatedString(context);
                }
            })
            .release()
            .concat().release()
            .choice().release()
            .concat().release()
            .action("terminalTokenIC", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleTerminalTokenIC(context);
                }
            })
            .pushReference("Spacing")
            .concat().release()
            .choice().release()
            .endProduction()
            
            .startProduction("SetExpression")
            .pushReference("SetElement")
            .action("terminalSet", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleTerminalSet(context);
                }
            })
            .endProduction()
            
            .startProduction("ClassExpression")
            .mark()
            .pushTokenCS("<")
            .pushReference("Spacing")
            .pushReference("Identifier")
            .pushTokenCS(">")
            .concat().release()
            .action("terminalClass", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleTerminalClass(context);
                }
            })
            .pushReference("Spacing")
            .endProduction()
            
            .startProduction("AnyExpression")
            .pushTokenCS(".")
            .action("terminalAny", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleTerminalAny(context);
                }
            })
            .pushReference("Spacing")
            .endProduction()
            
            .startProduction("SetElement")
            .mark()
            .pushReference("InverseSetElement")
            .action("setInverse", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSetInverse();
                }
            })
            .pushReference("IntervalElement")
            .action("setInterval", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSetInterval();
                }
            })
            .pushReference("CharacterElement")
            .action("setElement", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSetElement();
                }
            })
            .pushReference("NestedSetElement")
            .action("setUnion", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSetUnion();
                }
            })
            .choice().release()
            .endProduction()
            
            .startProduction("InverseSetElement")
            .pushTokenCS("~")
            .pushReference("Spacing")
            .pushReference("SetElement")
            .endProduction()
            
            .startProduction("IntervalElement")
            .mark()
            .pushReference("CharacterElement")
            .pushTokenCS("-")
            .pushReference("Spacing")
            .pushReference("CharacterElement")
            .concat().release()
            .endProduction()
            
            .startProduction("CharacterElement")
            .mark()
            .pushTokenCS("\'")
            .pushReference("Character")
            .action("decode", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleDecode(context);
                }
            })
            .pushTokenCS("\'")
            .concat().release()
            .pushReference("Spacing")
            .endProduction()
            
            .startProduction("NestedSetElement")
            .pushTokenCS("[")
            .pushReference("Spacing")
            .mark()
            .pushReference("SetElement")
            .mark()
            .pushTokenCS(",")
            .pushReference("Spacing")
            .pushReference("SetElement")
            .concat().release()
            .repeat(Quantifier.ZEROORMORE)
            .concat().release()
            .repeat(Quantifier.ONCEORNONE)
            .pushTokenCS("]")
            .pushReference("Spacing")
            .endProduction()
            
            .startProduction("Integer")
            .pushInterval('0', '9')
            .repeat(Quantifier.ONCEORMORE)
            .action("int", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleInt(context);
                }
            })
            .pushReference("Spacing")
            .endProduction()
            
            .startProduction("String")
            .mark()
            .mark()
            .pushTokenCS("\'")
            .pushReference("Character")
            .repeat(Quantifier.ZEROORMORE)
            .mark()
            .pushTokenCS("\'")
            .mark()
            .mark()
            .pushTokenCS("\n")
            .pushAny()
            .except(Predicate.NOT)
            .choice().release()
            .mark()
            .action("unterminatedString", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorUnterminatedString(context);
                }
            })
            .release()
            .concat().release()
            .choice().release()
            .concat().release()
            .mark()
            .pushTokenCS("\"")
            .pushReference("Character")
            .repeat(Quantifier.ZEROORMORE)
            .mark()
            .pushTokenCS("\"")
            .mark()
            .mark()
            .pushTokenCS("\n")
            .pushAny()
            .except(Predicate.NOT)
            .choice().release()
            .mark()
            .action("unterminatedString", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorUnterminatedString(context);
                }
            })
            .release()
            .concat().release()
            .choice().release()
            .concat().release()
            .choice().release()
            .endProduction()
            
            .startProduction("Character")
            .mark()
            .pushReference("EscapeSequence")
            .pushSet(UnicodeSet.fromElements('\\', '\'', '\"', '\n').inverse())
            .choice().release()
            .endProduction()
            
            .startProduction("EscapeSequence")
            .pushTokenCS("\\")
            .mark()
            .mark()
            .pushSet('\\', '\'', '\"', 'n', 'r', 't', 'f', 'b')
            .pushReference("UnicodeCharacter")
            .choice().release()
            .mark()
            .action("invalidEscapeSequence", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorInvalidEscapeSequence(context);
                }
            })
            .release()
            .choice().release()
            .endProduction()
            
            .startProduction("UnicodeCharacter")
            .pushTokenCS("u")
            .mark()
            .mark()
            .pushReference("HexDigit")
            .repeat(4).release()
            .mark()
            .action("invalidUnicodeCharacter", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorInvalidUnicodeCharacter(context);
                }
            })
            .release()
            .choice().release()
            .endProduction()
            
            .startProduction("HexDigit")
            .pushSet(UnicodeSet.fromIntervals('0', '9', 'a', 'f', 'A', 'F'))
            .endProduction()
            
            .startProduction("Identifier")
            .mark()
            .mark()
            .pushClass("ALPHA")
            .pushTokenCS("_")
            .choice().release()
            .mark()
            .pushClass("ALNUM")
            .pushTokenCS("_")
            .choice().release()
            .repeat(Quantifier.ZEROORMORE)
            .concat().release()
            .action("symbol", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleSymbol(context);
                }
            })
            .pushReference("Spacing")
            .endProduction()
            
            .startProduction("Comment")
            .mark()
            .pushReference("LineComment")
            .pushReference("BlockComment")
            .choice().release()
            .endProduction()
            
            .startProduction("LineComment")
            .pushTokenCS("//")
            .pushSet(UnicodeSet.fromElements('\n').inverse())
            .repeat(Quantifier.ZEROORMORE)
            .endProduction()
            
            .startProduction("BlockComment")
            .mark()
            .pushTokenCS("/*")
            .mark()
            .pushTokenCS("*/")
            .except(Predicate.NOT)
            .pushAny()
            .concat().release()
            .repeat(Quantifier.ZEROORMORE)
            .mark()
            .pushTokenCS("*/")
            .mark()
            .action("unterminatedBlockComment", new ActionHandler<GrammarParser>() {
                public boolean handle(GrammarParser parser, ActionContext context) {
                    return parser.handleErrorUnterminatedBlockComment(context);
                }
            })
            .release()
            .choice().release()
            .concat().release()
            .endProduction()
            
            .startProduction("Space")
            .pushSet(' ', '\t', '\n')
            .repeat(Quantifier.ONCEORMORE)
            .endProduction()
            
            .startProduction("Spacing")
            .mark()
            .pushReference("Space")
            .pushReference("Comment")
            .choice().release()
            .repeat(Quantifier.ZEROORMORE)
            .endProduction()
            
            .buildGrammar();
    
}
