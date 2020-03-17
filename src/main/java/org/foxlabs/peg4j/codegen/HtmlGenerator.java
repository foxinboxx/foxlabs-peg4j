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

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.foxlabs.common.Strings;
import org.foxlabs.peg4j.grammar.*;
import org.foxlabs.peg4j.resource.ResourceManager;

public final class HtmlGenerator extends BaseGenerator {

  // public static final int BUILD_STANDALONE = 0x01;
  public static final int SHOW_LINENUMBERS = 0x01;
  public static final int COMMENT_PROBLEMS = 0x02;
  public static final int PRINT_INJECTIONS = 0x04;
  public static final int HIGHLIGHTING_OFF = 0x08;

  private String title;

  private String theme;

  private int flags = 0;

  private boolean showLineNumbers;

  private boolean commentProblems;

  private boolean printInjections;

  private boolean highlightingOff;

  public HtmlGenerator() {
    this(null, SHOW_LINENUMBERS | COMMENT_PROBLEMS);
  }

  public HtmlGenerator(int flags) {
    this(null, flags);
  }

  public HtmlGenerator(String title, int flags) {
    this(title, null, flags);
  }

  public HtmlGenerator(String title, String theme, int flags) {
    this.title = title;
    this.theme = theme;
    setFlags(flags);
  }

  public int getFlags() {
    return flags;
  }

  public void setFlags(int flags) {
    this.flags = flags;
    this.showLineNumbers = (flags & SHOW_LINENUMBERS) != 0;
    this.commentProblems = (flags & COMMENT_PROBLEMS) != 0;
    this.printInjections = (flags & PRINT_INJECTIONS) != 0;
    this.highlightingOff = (flags & HIGHLIGHTING_OFF) != 0;
  }

  @Override
  protected String getTemplate() {
    return ResourceManager.getGrammarHtmlTemplate();
  }

  static int grammarObjSequence = 1;

  synchronized static String getGrammarObj() {
    return "Gx" + grammarObjSequence++;
  }

  static long tagIdSequence = 1;

  synchronized static String getId() {
    return "gx-" + tagIdSequence++;
  }

  static final class HtmlContext {

    final String grammarObj;

    final Grammar grammar;

    final Map<String, String> variables;

    HtmlContext(Grammar grammar, Map<String, String> variables) {
      this.grammarObj = getGrammarObj();
      this.grammar = grammar;
      this.variables = variables;
    }

  }

  @Override
  protected void defineVariables(Grammar grammar, Map<String, String> variables) {
    super.defineVariables(grammar, variables);
    HtmlContext hc = new HtmlContext(grammar, variables);
    defineTitle(hc);
    defineGrammarTheme(hc);
    defineGrammar(hc);
  }

  // ${title}
  private void defineTitle(HtmlContext hc) {
    String title = this.title;
    if (title == null) {
      title = hc.grammar.getFile();
      if (title == null) {
        title = "Unknown Grammar";
      }
    }
    hc.variables.put("title", encodeHtml(title));
  }

  // ${grammar_theme}
  private void defineGrammarTheme(HtmlContext hc) {
    hc.variables.put("grammar_theme", theme == null ? "" : theme);
  }

  // ${grammar_obj}
  // ${grammar_data}
  // ${grammar_tags}
  private void defineGrammar(HtmlContext hc) {
    RuleWriter rw = new RuleWriter(hc);

    rw.appendHeader();
    int count = hc.grammar.getProductionCount();
    for (int i = 0; i < count; i++) {
      Production rule = hc.grammar.getProduction(i);
      if (!(rule.getExpression() instanceof Terminal.Nil)) {
        rule.accept(rw);
      }
    }
    rw.appendFooter();

    StringBuilder dataStmts = new StringBuilder();
    int size = rw.highlightSets.size();
    if (size > 0) {
      rw.highlightSets.get(0).toString(dataStmts);
      for (int i = 1; i < size; i++) {
        dataStmts.append(", ");
        rw.highlightSets.get(i).toString(dataStmts);
      }
    }

    hc.variables.put("grammar_obj", hc.grammarObj);
    hc.variables.put("grammar_data", dataStmts.toString());
    hc.variables.put("grammar_tags", rw.tags.toString());
  }

  static final class HighlightSet {

    final int index;

    final String clazz;

    final Set<String> idSet;

    HighlightSet(int index, String clazz) {
      this.index = index;
      this.clazz = clazz;
      this.idSet = new HashSet<String>();
    }

    public String addElement() {
      String id = getId();
      idSet.add(id);
      return id;
    }

    public void addElement(String id) {
      idSet.add(id);
    }

    public void toString(StringBuilder buf) {
      buf.append("{\n");
      appendIdent(4, buf);
      buf.append("cls : \'");
      buf.append(clazz);
      buf.append("\',\n");
      appendIdent(4, buf);
      buf.append("set : [");
      Iterator<String> itr = idSet.iterator();
      if (itr.hasNext()) {
        buf.append("\'");
        buf.append(itr.next());
        buf.append("\'");
        while (itr.hasNext()) {
          buf.append(", \'");
          buf.append(itr.next());
          buf.append("\'");
        }
      }
      buf.append("]\n}");
    }

  }

  // RuleWriter

  final class RuleWriter implements RuleVisitor<RuntimeException> {

    final HtmlContext hc;

    final StringBuilder tags = new StringBuilder();

    final List<HighlightSet> highlightSets = new ArrayList<HighlightSet>();

    final Map<String, HighlightSet> classSets = new HashMap<String, HighlightSet>();

    final Map<String, HighlightSet> actionSets = new HashMap<String, HighlightSet>();

    final Map<String, HighlightSet> productionSets = new HashMap<String, HighlightSet>();

    final Map<Problem, HighlightSet> problemSets = new HashMap<Problem, HighlightSet>();

    final Map<String, String> productionIds = new HashMap<String, String>();

    final Map<Problem, String> problemIds = new HashMap<Problem, String>();

    boolean started = false;

    int line = 1;

    RuleWriter(HtmlContext hc) {
      this.hc = hc;
      int count = hc.grammar.getProductionCount();
      for (int i = 0; i < count; i++) {
        Production rule = hc.grammar.getProduction(i);
        String name = rule.getName();
        HighlightSet set = addHighlightSet();
        String productionId = set.addElement();
        productionSets.put(name, set);
        productionIds.put(name, productionId);
      }
    }

    public void visit(Terminal rule) {
      if (rule instanceof Terminal.Token) {
        Terminal.Token term = (Terminal.Token) rule;
        String quote = term.isCaseSensitive() ? "\'" : "\"";
        appendString(term.getImage(), quote);
      } else if (rule instanceof Terminal.Interval) {
        Terminal.Interval term = (Terminal.Interval) rule;
        appendString(Character.toString((char) term.getMin()), "\'");
        appendSymbol("-", null);
        appendString(Character.toString((char) term.getMax()), "\'");
      } else if (rule instanceof Terminal.Set) {
        Terminal.Set term = (Terminal.Set) rule;
        HighlightSet set = addHighlightSet();
        int[] intervals = term.getUnicodeSet().toArray();
        appendSymbol("[", set);
        for (int i = 0; i < intervals.length;) {
          char min = (char) intervals[i++];
          char max = (char) intervals[i++];
          appendString(Character.toString(min), "\'");
          if (min < max) {
            appendSymbol("-", null);
            appendString(Character.toString(max), "\'");
          }
          if (i < intervals.length) {
            appendSymbol(",", null);
            appendIdent(1);
          }
        }
        appendSymbol("]", set);
      } else if (rule instanceof Terminal.Class) {
        Terminal.Class term = (Terminal.Class) rule;
        String name = term.getName();
        HighlightSet set1 = addHighlightSet();
        HighlightSet set2 = classSets.get(name);
        if (set2 == null) {
          set2 = addHighlightSet();
          classSets.put(name, set2);
        }
        appendSymbol("<", set1);
        appendText(name, set2, "gx-rule-class");
        appendSymbol(">", set1);
      } else if (rule instanceof Terminal.Any) {
        appendText(".", null, "gx-rule-any");
      }
    }

    public void visit(Production rule) {
      tags.append("<!--\u0020");
      tags.append(rule.getName());
      tags.append("\u0020-->\n");
      appendStart();
      appendProblemComment(rule.getAllProblems());
      String name = rule.getName();
      String id = productionIds.get(name);
      HighlightSet set = productionSets.get(name);
      appendProblemStart(rule.getProblems());
      appendText(id, name, set, "gx-rule-production");
      appendProblemEnd(rule.getProblems());
      int length = name.length();
      if (length < 4) {
        appendIdent(4 - length);
      } else {
        appendLine();
        appendIdent(4);
      }
      appendSymbol(":", null);
      appendIdent(4);
      appendRule(rule.getExpression(), false);
      appendLine();
      appendIdent(4);
      appendSymbol(";", null);
      appendLine();
      appendIdent(4);
      appendEnd();
    }

    public void visit(Reference rule) {
      String name = rule.getTargetName();
      HighlightSet set = productionSets.get(name);
      Modifier mod = rule.getModifier();
      String text = mod == null ? name : mod.toString() + name;
      String href = "#" + productionIds.get(name);
      appendAnchor(text, href, set, "gx-rule-reference");
    }

    public void visit(Action rule) {
      Expression child = rule.getChild();
      boolean injected = rule.isInjected();
      if (!injected || printInjections) {
        String name = rule.getName();
        HighlightSet set = actionSets.get(name);
        if (set == null) {
          set = addHighlightSet();
          actionSets.put(name, set);
        }
        appendText("$" + name, set, injected ? "gx-injection" : "gx-rule-action");
        appendRule(child, true);
      } else {
        appendRule(child, child instanceof Expression.Nary);
      }
    }

    public void visit(Concatenation rule) {
      HighlightSet set = addHighlightSet();
      Rule operand = rule.getChild(0);
      appendRule(operand, operand instanceof Expression.Nary);
      int length = rule.length();
      for (int i = 1; i < length; i++) {
        operand = rule.getChild(i);
        appendOperator(" ", set);
        appendRule(operand, operand instanceof Expression.Nary);
      }
    }

    public void visit(Alternation rule) {
      HighlightSet set = addHighlightSet();
      Rule operand = rule.getChild(0);
      appendRule(operand, operand instanceof Expression.Nary);
      int length = rule.length();
      /*
       * if (rule.getParent() == null) { for (int i = 1; i < length; i++) {
       * operand = rule.getRule(i); appendLine(); appendIdent(4);
       * appendOperator("/", set); appendIdent(4); appendRule(operand, operand
       * instanceof Expression.Nary); } } else {
       */
      for (int i = 1; i < length; i++) {
        operand = rule.getChild(i);
        appendIdent(1);
        appendOperator("/", set);
        appendIdent(1);
        appendRule(operand, operand instanceof Expression.Nary);
      }
      // }
    }

    public void visit(Repetition rule) {
      Rule operand = rule.getChild();
      appendRule(operand, operand instanceof Operator);
      Quantifier quant = rule.getQuantifier();
      if (quant == null) {
        int min = rule.getMin();
        int max = rule.getMax();
        HighlightSet set = addHighlightSet();
        appendSymbol("{", set);
        appendInteger(min);
        if (min < max) {
          appendSymbol(",", null);
          if (max < Integer.MAX_VALUE) {
            appendIdent(1);
            appendInteger(max);
          }
        }
        appendSymbol("}", set);
      } else {
        appendOperator(quant.toString(), null);
      }
    }

    public void visit(Exclusion rule) {
      appendOperator(rule.getPredicate().toString(), null);
      Rule operand = rule.getChild();
      appendRule(operand, operand instanceof Operator);
    }

    private HighlightSet addHighlightSet() {
      return addHighlightSet("gx-highlighting");
    }

    private HighlightSet addHighlightSet(String clazz) {
      HighlightSet set = new HighlightSet(highlightSets.size(), clazz);
      highlightSets.add(set);
      return set;
    }

    private void appendHeader() {
      tags.append("<table");
      appendTagClass("gx-code");
      tags.append("\u0020cellspacing=\"0\"\u0020cellpadding=\"2\">\n\n");

      appendStart();

      appendCommentText("//\u0020");
      appendAnchor(ResourceManager.getProductURL());
      appendLine();

      appendEnd();
    }

    private void appendFooter() {
      tags.append("</table>\n");
    }

    private void appendStart() {
      started = false;
      appendLine();
    }

    private void appendEnd() {
      tags.append("\n</td>\n</tr>\n\n");
    }

    private void appendLine() {
      if (started) {
        tags.append("\n</td>\n</tr>\n");
      } else {
        started = true;
      }
      tags.append("<tr>\n");
      if (showLineNumbers) {
        tags.append("<td");
        appendTagClass("gx-line");
        tags.append("\u0020nowrap=\"nowrap\">");
        tags.append(line++);
        tags.append("</td>\n");
      }
      tags.append("<td\u0020width=\"100%\"\u0020nowrap=\"nowrap\">\n");
    }

    private void appendIdent(int size) {
      for (int i = 0; i < size; i++) {
        tags.append("&nbsp;");
      }
    }

    private void appendTagId(String id) {
      if (id != null) {
        tags.append("\u0020id=\"");
        tags.append(id);
        tags.append("\"");
      }
    }

    private void appendTagClass(String clazz) {
      if (clazz != null) {
        tags.append("\u0020class=\"");
        tags.append(clazz);
        tags.append("\"");
      }
    }

    private void appendTagClasses(String... clazzez) {
      if (clazzez.length > 0) {
        tags.append("\u0020class=\"");
        tags.append(clazzez[0]);
        for (int i = 1; i < clazzez.length; i++) {
          tags.append("\u0020");
          tags.append(clazzez[i]);
        }
        tags.append("\"");
      }
    }

    private void appendTagHighlighting(HighlightSet set) {
      if (!(highlightingOff || set == null)) {
        tags.append("\u0020onmouseover=\"");
        tags.append(hc.grammarObj);
        tags.append(".on(");
        tags.append(set.index);
        tags.append(")\"\u0020onmouseout=\"");
        tags.append(hc.grammarObj);
        tags.append(".off(");
        tags.append(set.index);
        tags.append(")\"");
      }
    }

    private void appendText(String text, HighlightSet set, String... clazzez) {
      String id = set == null ? null : set.addElement();
      appendText(id, text, set, clazzez);
    }

    private void appendText(String id, String text, HighlightSet set, String... clazzez) {
      tags.append("<span");
      appendTagId(id);
      appendTagClasses(clazzez);
      appendTagHighlighting(set);
      tags.append(">");
      encodeHtml(text, tags);
      tags.append("</span>");
    }

    private void appendSymbol(String symbol, HighlightSet set) {
      appendText(symbol, set, "gx-symbol");
    }

    private void appendOperator(String symbol, HighlightSet set) {
      appendText(symbol, set, "gx-operator");
    }

    private void appendString(String text, String quote) {
      appendText(quote + Strings.escape(text) + quote, null, "gx-string");
    }

    private void appendInteger(int value) {
      appendText(Integer.toString(value), null, "gx-number");
    }

    private void appendCommentText(String text) {
      appendCommentText(null, text, null);
    }

    private void appendCommentText(String id, String text, HighlightSet set) {
      appendText(id, text, set, "gx-comment");
    }

    private void appendAnchor(String text) {
      appendAnchor(text, text, null);
    }

    private void appendAnchor(String text, String href, HighlightSet set, String... clazzez) {
      tags.append("<a");
      if (set != null) {
        appendTagId(set.addElement());
      }
      appendTagClasses(clazzez);
      appendTagHighlighting(set);
      tags.append("\u0020href=\"");
      tags.append(href);
      tags.append("\"");
      tags.append(">");
      encodeHtml(text, tags);
      tags.append("</a>");
    }

    private void appendRule(Rule rule, boolean parenthesize) {
      List<Problem> problems = rule.getProblems();
      appendProblemStart(problems);
      if (parenthesize) {
        HighlightSet set = addHighlightSet();
        appendSymbol("(", set);
        rule.accept(this);
        appendSymbol(")", set);
      } else {
        rule.accept(this);
      }
      appendProblemEnd(problems);
    }

    private void appendProblemComment(List<Problem> problems) {
      if (commentProblems) {
        problemIds.clear();
        problemSets.clear();
        if (problems.size() > 0) {
          appendCommentText("/*");
          appendLine();
          for (Problem problem : problems) {
            HighlightSet set = addHighlightSet();
            problemSets.put(problem, set);
            String id = set.addElement();
            problemIds.put(problem, id);
            String message = problem.getCode().getType() + ": " + problem.getMessage();
            appendIdent(1);
            appendCommentText("*");
            appendIdent(1);
            appendCommentText(id, message, set);
            appendLine();
          }
          appendIdent(1);
          appendCommentText("*/");
          appendLine();
        }
      }
    }

    private void appendProblemStart(List<Problem> problems) {
      if (commentProblems && problems.size() > 0) {
        HighlightSet ruleSet = addHighlightSet();
        String id = ruleSet.addElement();
        Problem.Type worst = problems.get(0).getCode().getType();
        for (Problem problem : problems) {
          String problemId = problemIds.get(problem);
          if (problemId != null) {
            ruleSet.addElement(problemId);
          }
          HighlightSet problemSet = problemSets.get(problem);
          if (problemSet != null) {
            problemSet.addElement(id);
          }
          Problem.Type type = problem.getCode().getType();
          if (type.ordinal() < worst.ordinal()) {
            worst = type;
          }
        }
        tags.append("<span");
        appendTagId(id);
        appendTagClass(PROBLEM_CLASSES[worst.ordinal()]);
        appendTagHighlighting(ruleSet);
        tags.append(">");
      }
    }

    private void appendProblemEnd(List<Problem> problems) {
      if (commentProblems && problems.size() > 0) {
        tags.append("</span>");
      }
    }

  }

  static final String[] PROBLEM_CLASSES = new String[] {
      "gx-problem-fatal",
      "gx-problem-error",
      "gx-problem-warning",
      "gx-problem-hint"
  };

  // Utils

  static String encodeHtml(String text) {
    StringBuilder buf = new StringBuilder();
    encodeHtml(text, buf);
    return buf.toString();
  }

  static void encodeHtml(String text, StringBuilder buf) {
    if (text != null) {
      int length = text.length();
      for (int i = 0; i < length; i++) {
        char ch = text.charAt(i);
        switch (ch) {
        case '&':
        case '<':
        case '>':
          buf.append('&').append('#').append((int) ch).append(';');
          break;
        default:
          buf.append(ch);
          break;
        }
      }
    }
  }

}
