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
import org.foxlabs.peg4j.util.BacktrackingStack;

import org.foxlabs.util.Location;
import org.foxlabs.util.Strings;
import org.foxlabs.util.UnicodeSet;

import static org.foxlabs.peg4j.grammar.Problem.Code.*;

public final class GrammarParser extends Parser<Grammar> {

  private final GrammarBuilder builder;

  private final BacktrackingStack<String> symbolStack = new BacktrackingStack<String>();

  private final BacktrackingStack<Integer> intStack = new BacktrackingStack<Integer>();

  private final BacktrackingStack<UnicodeSet> usetStack = new BacktrackingStack<UnicodeSet>();

  private final BacktrackingStack<Problem> problemStack = new BacktrackingStack<Problem>();

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
      // Grammar
      .startProduction("Grammar")
      .mark()
      .mark()
      .pushReference("Production", Modifier.MEMO)
      .mark()
      .pushReference("Spacing")
      .pushReference("Garbage")
      .action("startError",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorStart(context))
      .mark()
      .pushReference("Production", Modifier.MEMO)
      .except(Predicate.NOT)
      .pushReference("Garbage")
      .concat().release()
      .repeat(Quantifier.ZEROORMORE)
      .mark()
      .action("endError",
          (GrammarParser parser, ActionContext context) -> parser.handleEndError(context))
      .release()
      .concat().release()
      .choice().release()
      .repeat(Quantifier.ZEROORMORE)
      .pushReference("Spacing")
      .pushAny()
      .except(Predicate.NOT)
      .action("eof",
          (GrammarParser parser, ActionContext context) -> parser.handleEof(context))
      .concat().release()
      .action("source",
          (GrammarParser parser, ActionContext context) -> parser.handleSource(context))
      .endProduction()
      // Garbage
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
      // Production
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
      .action("startProduction",
          (GrammarParser parser, ActionContext context) -> parser.handleProductionStart(context))
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
      .action("invalidExpression",
          (GrammarParser parser, ActionContext context) -> parser.handleInvalidExpression(context))
      .pushReference("Expression", Modifier.MEMO)
      .pushReference("Comment", Modifier.MEMO)
      .pushReference("Space", Modifier.MEMO)
      .choice().release()
      .repeat(Quantifier.ZEROORMORE)
      .mark()
      .pushTokenCS(";")
      .mark()
      .action("missingSemi",
          (GrammarParser parser, ActionContext context) -> parser.handleMissingSemi(context))
      .release()
      .choice().release()
      .concat().release()
      .choice().release()
      .action("endProduction",
          (GrammarParser parser, ActionContext context) -> parser.handleProductionEnd(context))
      .concat().release()
      .endProduction()
      // Expression
      .startProduction("Expression")
      .pushReference("AlternationExpression")
      .endProduction()
      // AlternationExpression
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
      .action("ruleChoice",
          (GrammarParser parser, ActionContext context) -> parser.handleRuleChoice(context))
      .endProduction()
      // ConcatenationExpression
      .startProduction("ConcatenationExpression")
      .pushReference("RepetitionExpression")
      .repeat(Quantifier.ONCEORMORE)
      .action("ruleConcat",
          (GrammarParser parser, ActionContext context) -> parser.handleRuleConcat(context))
      .endProduction()
      // RepetitionExpression
      .startProduction("RepetitionExpression")
      .mark()
      .pushReference("ExclusionExpression")
      .mark()
      .mark()
      .pushSet(UnicodeSet.fromIntervals(new int[]{'*', '+', '?', '?'}))
      .action("symbol",
          (GrammarParser parser, ActionContext context) -> parser.handleSymbol(context))
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
      .action("unlimited",
          (GrammarParser parser, ActionContext context) -> parser.handleUnlimited())
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
      .action("ruleRepeat",
          (GrammarParser parser, ActionContext context) -> parser.handleRuleRepeat(context))
      .endProduction()
      // ExclusionExpression
      .startProduction("ExclusionExpression")
      .mark()
      .mark()
      .mark()
      .pushTokenCS(Predicate.NOT.toString())
      .pushTokenCS(Predicate.AND.toString())
      .choice().release()
      .action("symbol",
          (GrammarParser parser, ActionContext context) -> parser.handleSymbol(context))
      .pushReference("Spacing")
      .concat().release()
      .repeat(Quantifier.ONCEORNONE)
      .pushReference("OperandExpression")
      .concat().release()
      .action("ruleExcept",
          (GrammarParser parser, ActionContext context) -> parser.handleRuleExcept(context))
      .endProduction()
      // OperandExpression
      .startProduction("OperandExpression")
      .mark()
      .pushReference("TerminalExpression")
      .pushReference("ReferenceExpression")
      .pushReference("GroupingExpression")
      .pushReference("ActionExpression")
      .choice().release()
      .endProduction()
      // ReferenceExpression
      .startProduction("ReferenceExpression")
      .mark()
      .mark()
      .pushTokenCS(Modifier.MEMO.toString())
      .choice().release()
      .action("symbol",
          (GrammarParser parser, ActionContext context) -> parser.handleSymbol(context))
      .repeat(Quantifier.ONCEORNONE)
      .pushReference("Identifier")
      .concat().release()
      .action("ruleReference",
          (GrammarParser parser, ActionContext context) -> parser.handleRuleReference(context))
      .endProduction()
      // GroupingExpression
      .startProduction("GroupingExpression")
      .mark()
      .pushTokenCS("(")
      .pushReference("Spacing")
      .pushReference("Expression")
      .mark()
      .pushTokenCS(")")
      .mark()
      .action("missingClosingParenthesize",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorMissingClosingParenthesize(context))
      .release()
      .choice().release()
      .concat().release()
      .pushReference("Spacing")
      .endProduction()
      // ActionExpression
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
      .action("ruleAction",
          (GrammarParser parser, ActionContext context) -> parser.handleRuleAction(context))
      .pushReference("Spacing")
      .endProduction()
      // TerminalExpression
      .startProduction("TerminalExpression")
      .mark()
      .pushReference("IntervalExpression")
      .pushReference("TokenExpression")
      .pushReference("SetExpression")
      .pushReference("ClassExpression")
      .pushReference("AnyExpression")
      .choice().release()
      .endProduction()
      // IntervalExpression
      .startProduction("IntervalExpression")
      .pushReference("IntervalElement")
      .action("terminalInterval",
          (GrammarParser parser, ActionContext context) -> parser.handleTerminalInterval(context))
      .endProduction()
      // TokenExpression
      .startProduction("TokenExpression")
      .mark()
      .mark()
      .mark()
      .pushTokenCS("\'")
      .pushReference("Character")
      .repeat(Quantifier.ONCEORMORE)
      .action("decode",
          (GrammarParser parser, ActionContext context) -> parser.handleDecode(context))
      .mark()
      .pushTokenCS("\'")
      .mark()
      .mark()
      .pushTokenCS("\n")
      .pushAny()
      .except(Predicate.NOT)
      .choice().release()
      .mark()
      .action("unterminatedString",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorUnterminatedString(context))
      .release()
      .concat().release()
      .choice().release()
      .concat().release()
      .action("terminalTokenCS",
          (GrammarParser parser, ActionContext context) -> parser.handleTerminalTokenCS(context))
      .pushReference("Spacing")
      .concat().release()
      .mark()
      .mark()
      .pushTokenCS("\"")
      .pushReference("Character")
      .repeat(Quantifier.ONCEORMORE)
      .action("decode",
          (GrammarParser parser, ActionContext context) -> parser.handleDecode(context))
      .mark()
      .pushTokenCS("\"")
      .mark()
      .mark()
      .pushTokenCS("\n")
      .pushAny()
      .except(Predicate.NOT)
      .choice().release()
      .mark()
      .action("unterminatedString",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorUnterminatedString(context))
      .release()
      .concat().release()
      .choice().release()
      .concat().release()
      .action("terminalTokenIC",
          (GrammarParser parser, ActionContext context) -> parser.handleTerminalTokenIC(context))
      .pushReference("Spacing")
      .concat().release()
      .choice().release()
      .endProduction()
      // SetExpression
      .startProduction("SetExpression")
      .pushReference("SetElement")
      .action("terminalSet",
          (GrammarParser parser, ActionContext context) -> parser.handleTerminalSet(context))
      .endProduction()
      // ClassExpression
      .startProduction("ClassExpression")
      .mark()
      .pushTokenCS("<")
      .pushReference("Spacing")
      .pushReference("Identifier")
      .pushTokenCS(">")
      .concat().release()
      .action("terminalClass",
          (GrammarParser parser, ActionContext context) -> parser.handleTerminalClass(context))
      .pushReference("Spacing")
      .endProduction()
      // AnyExpression
      .startProduction("AnyExpression")
      .pushTokenCS(".")
      .action("terminalAny",
          (GrammarParser parser, ActionContext context) -> parser.handleTerminalAny(context))
      .pushReference("Spacing")
      .endProduction()
      // SetElement
      .startProduction("SetElement")
      .mark()
      .pushReference("InverseSetElement")
      .action("setInverse",
          (GrammarParser parser, ActionContext context) -> parser.handleSetInverse())
      .pushReference("IntervalElement")
      .action("setInterval",
          (GrammarParser parser, ActionContext context) -> parser.handleSetInterval())
      .pushReference("CharacterElement")
      .action("setElement",
          (GrammarParser parser, ActionContext context) -> parser.handleSetElement())
      .pushReference("NestedSetElement")
      .action("setUnion",
          (GrammarParser parser, ActionContext context) -> parser.handleSetUnion())
      .choice().release()
      .endProduction()
      // InverseSetElement
      .startProduction("InverseSetElement")
      .pushTokenCS("~")
      .pushReference("Spacing")
      .pushReference("SetElement")
      .endProduction()
      // IntervalElement
      .startProduction("IntervalElement")
      .mark()
      .pushReference("CharacterElement")
      .pushTokenCS("-")
      .pushReference("Spacing")
      .pushReference("CharacterElement")
      .concat().release()
      .endProduction()
      // CharacterElement
      .startProduction("CharacterElement")
      .mark()
      .pushTokenCS("\'")
      .pushReference("Character")
      .action("decode",
          (GrammarParser parser, ActionContext context) -> parser.handleDecode(context))
      .pushTokenCS("\'")
      .concat().release()
      .pushReference("Spacing")
      .endProduction()
      // NestedSetElement
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
      // Integer
      .startProduction("Integer")
      .pushInterval('0', '9')
      .repeat(Quantifier.ONCEORMORE)
      .action("int",
          (GrammarParser parser, ActionContext context) -> parser.handleInt(context))
      .pushReference("Spacing")
      .endProduction()
      // String
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
      .action("unterminatedString",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorUnterminatedString(context))
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
      .action("unterminatedString",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorUnterminatedString(context))
      .release()
      .concat().release()
      .choice().release()
      .concat().release()
      .choice().release()
      .endProduction()
      // Character
      .startProduction("Character")
      .mark()
      .pushReference("EscapeSequence")
      .pushSet(UnicodeSet.fromElements('\\', '\'', '\"', '\n').inverse())
      .choice().release()
      .endProduction()
      // EscapeSequence
      .startProduction("EscapeSequence")
      .pushTokenCS("\\")
      .mark()
      .mark()
      .pushSet('\\', '\'', '\"', 'n', 'r', 't', 'f', 'b')
      .pushReference("UnicodeCharacter")
      .choice().release()
      .mark()
      .action("invalidEscapeSequence",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorInvalidEscapeSequence(context))
      .release()
      .choice().release()
      .endProduction()
      // UnicodeCharacter
      .startProduction("UnicodeCharacter")
      .pushTokenCS("u")
      .mark()
      .mark()
      .pushReference("HexDigit")
      .repeat(4).release()
      .mark()
      .action("invalidUnicodeCharacter",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorInvalidUnicodeCharacter(context))
      .release()
      .choice().release()
      .endProduction()
      // HexDigit
      .startProduction("HexDigit")
      .pushSet(UnicodeSet.fromIntervals('0', '9', 'a', 'f', 'A', 'F'))
      .endProduction()
      // Identifier
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
      .action("symbol",
          (GrammarParser parser, ActionContext context) -> parser.handleSymbol(context))
      .pushReference("Spacing")
      .endProduction()
      // Comment
      .startProduction("Comment")
      .mark()
      .pushReference("LineComment")
      .pushReference("BlockComment")
      .choice().release()
      .endProduction()
      // LineComment
      .startProduction("LineComment")
      .pushTokenCS("//")
      .pushSet(UnicodeSet.fromElements('\n').inverse())
      .repeat(Quantifier.ZEROORMORE)
      .endProduction()
      // BlockComment
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
      .action("unterminatedBlockComment",
          (GrammarParser parser, ActionContext context) -> parser.handleErrorUnterminatedBlockComment(context))
      .release()
      .choice().release()
      .concat().release()
      .endProduction()
      // Space
      .startProduction("Space")
      .pushSet(' ', '\t', '\n')
      .repeat(Quantifier.ONCEORMORE)
      .endProduction()
      // Spacing
      .startProduction("Spacing")
      .mark()
      .pushReference("Space")
      .pushReference("Comment")
      .choice().release()
      .repeat(Quantifier.ZEROORMORE)
      .endProduction()
      // !
      .buildGrammar();
    
}
