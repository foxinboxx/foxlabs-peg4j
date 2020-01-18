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
import java.util.StringTokenizer;

import org.foxlabs.peg4j.grammar.*;
import org.foxlabs.peg4j.resource.ResourceManager;

import org.foxlabs.util.Location;
import org.foxlabs.util.Strings;

public final class TextGenerator extends BaseGenerator {

  public static final int GENERATE_COMMENTS = 0x08;

  private int flags;

  private boolean generateComments;

  public TextGenerator() {
    this(GENERATE_COMMENTS);
  }

  public TextGenerator(int flags) {
    setFlags(flags);
  }

  public int getFlags() {
    return flags;
  }

  public void setFlags(int flags) {
    this.flags = flags;
    this.generateComments = (flags & GENERATE_COMMENTS) != 0;
  }

  @Override
  protected String getTemplate() {
    return ResourceManager.getGrammarTextTemplate();
  }

  static final class TextContext {

    final Grammar grammar;

    final Map<String, String> variables;

    TextContext(Grammar grammar, Map<String, String> variables) {
      this.grammar = grammar;
      this.variables = variables;
    }

  }

  @Override
  protected void defineVariables(Grammar grammar, Map<String, String> variables) {
    super.defineVariables(grammar, variables);
    TextContext tc = new TextContext(grammar, variables);
    defineGrammarStatements(tc);
    defineGrammarProblems(tc);
  }

  private void defineGrammarStatements(TextContext tc) {
    RuleWriter rw = new RuleWriter(tc);
    int count = tc.grammar.getProductionCount();
    for (int i = 0; i < count; i++) {
      Production rule = tc.grammar.getProduction(i);
      if (!(rule.getExpression() instanceof Terminal.Nil)) {
        rule.accept(rw);
      }
    }
    tc.variables.put("grammar_statements", rw.statements.toString());
  }

  private void defineGrammarProblems(TextContext tc) {
    StringBuilder comments = new StringBuilder();
    if (generateComments && tc.grammar.hasProblems()) {
      appendComment("PROBLEMS:", comments);
      comments.append("\n");
      for (Problem problem : tc.grammar.getProblems().getProblems())
        if (Location.UNKNOWN != problem.getStart()) {
          appendComment(problem.toString(), comments);
          comments.append("\n");
        }
    }
    tc.variables.put("grammar_problems", comments.toString());
  }

  final class RuleWriter implements RuleVisitor<RuntimeException> {

    final TextContext tc;

    final StringBuilder statements = new StringBuilder();

    RuleWriter(TextContext tc) {
      this.tc = tc;
    }

    public void visit(Terminal rule) {
      if (rule instanceof Terminal.Token) {
        Terminal.Token term = (Terminal.Token) rule;
        if (term.isCaseSensitive()) {
          statements.append('\'');
          Strings.escape(term.getImage(), statements);
          statements.append('\'');
        } else {
          statements.append('\"');
          Strings.escape(term.getImage(), statements);
          statements.append('\"');
        }
      } else if (rule instanceof Terminal.Interval) {
        Terminal.Interval term = (Terminal.Interval) rule;
        statements.append('\'');
        statements.append(Strings.escape((char) term.getMin()));
        statements.append("\'-\'");
        statements.append(Strings.escape((char) term.getMax()));
        statements.append('\'');
      } else if (rule instanceof Terminal.Set) {
        Terminal.Set term = (Terminal.Set) rule;
        statements.append('[');
        int[] intervals = term.getUnicodeSet().toArray();
        for (int i = 0; i < intervals.length;) {
          char min = (char) intervals[i++];
          char max = (char) intervals[i++];
          statements.append('\'');
          statements.append(Strings.escape(min));
          statements.append('\'');
          if (min < max) {
            statements.append('-');
            statements.append('\'');
            statements.append(Strings.escape(max));
            statements.append('\'');
          }
          if (i < intervals.length) {
            statements.append(',').append(' ');
          }
        }
        statements.append(']');
      } else if (rule instanceof Terminal.Class) {
        Terminal.Class term = (Terminal.Class) rule;
        statements.append('<');
        statements.append(term.getName());
        statements.append('>');
      } else if (rule instanceof Terminal.Any) {
        statements.append('.');
      }
    }

    public void visit(Production rule) {
      if (generateComments) {
        for (Problem problem : rule.getAllProblems()) {
          if (Location.UNKNOWN != problem.getStart()) {
            appendComment(problem.toString(), statements);
          }
        }
      }
      String name = rule.getName();
      statements.append(name);
      if (name.length() < 4) {
        appendIdent(4 - name.length(), statements);
      } else {
        statements.append('\n');
        appendIdent(4, statements);
      }
      statements.append(':');
      appendIdent(4, statements);
      appendRule(rule.getExpression(), false);
      statements.append('\n');
      appendIdent(4, statements);
      statements.append(';');
      statements.append('\n');
      appendIdent(4, statements);
      statements.append('\n');
    }

    public void visit(Reference rule) {
      statements.append(rule.getTargetName());
    }

    public void visit(Action rule) {
      Rule operand = rule.getChild();
      if (rule.isInjected()) {
        appendRule(operand, operand instanceof Expression.Nary);
      } else {
        statements.append('$');
        statements.append(rule.getName());
        appendRule(operand, true);
      }
    }

    public void visit(Concatenation rule) {
      Rule operand = rule.getChild(0);
      appendRule(operand, operand instanceof Expression.Nary);
      int length = rule.length();
      for (int i = 1; i < length; i++) {
        operand = rule.getChild(i);
        statements.append(' ');
        appendRule(operand, operand instanceof Expression.Nary);
      }
    }

    public void visit(Alternation rule) {
      Rule operand = rule.getChild(0);
      appendRule(operand, operand instanceof Expression.Nary);
      int length = rule.length();
      if (rule.getParent() == null) {
        for (int i = 1; i < length; i++) {
          operand = rule.getChild(i);
          statements.append('\n');
          appendIdent(4, statements);
          statements.append('/');
          appendIdent(4, statements);
          appendRule(operand, operand instanceof Expression.Nary);
        }
      } else {
        for (int i = 1; i < length; i++) {
          operand = rule.getChild(i);
          statements.append(" / ");
          appendRule(operand, operand instanceof Expression.Nary);
        }
      }
    }

    public void visit(Repetition rule) {
      Rule operand = rule.getChild();
      appendRule(operand, operand instanceof Operator);
      Quantifier quant = rule.getQuantifier();
      if (quant == null) {
        int min = rule.getMin();
        int max = rule.getMax();
        statements.append('{');
        statements.append(min);
        if (min < max) {
          statements.append(',');
          if (max < Integer.MAX_VALUE) {
            statements.append(' ').append(max);
          }
        }
        statements.append('}');
      } else {
        statements.append(quant);
      }
    }

    public void visit(Exclusion rule) {
      statements.append(rule.getPredicate());
      Rule operand = rule.getChild();
      appendRule(operand, operand instanceof Operator);
    }

    private void appendRule(Rule rule, boolean parenthesize) {
      if (parenthesize) {
        statements.append('(');
        rule.accept(this);
        statements.append(')');
      } else {
        rule.accept(this);
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

}
