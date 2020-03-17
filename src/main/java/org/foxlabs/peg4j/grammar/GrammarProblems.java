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
import java.util.Collections;

import org.foxlabs.common.function.ToString;
import org.foxlabs.common.text.CharBuffer;
import org.foxlabs.util.Location;

public class GrammarProblems extends RuntimeException implements ToString {

  private static final long serialVersionUID = -2577809311036208714L;

  private List<Problem> problems = new ArrayList<Problem>();

  private List<Problem> fatals = new ArrayList<Problem>();

  private List<Problem> errors = new ArrayList<Problem>();

  private List<Problem> warnings = new ArrayList<Problem>();

  private List<Problem> hints = new ArrayList<Problem>();

  GrammarProblems() {
    super();
  }

  void add(Problem.Code code, Location start, Location end) {
    add(new Problem(code, start, end));
  }

  void add(Problem.Code code, Location start, Location end, String... attributes) {
    add(new Problem(code, start, end, attributes));
  }

  void add(Problem.Code code, Rule rule) {
    add(new Problem(code, rule));
  }

  void add(Problem.Code code, Rule rule, String... attributes) {
    add(new Problem(code, rule, attributes));
  }

  void add(Problem problem) {
    problems.add(problem);
    switch (problem.getType()) {
    case FATAL:
      fatals.add(problem);
      break;
    case ERROR:
      errors.add(problem);
      break;
    case WARNING:
      warnings.add(problem);
      break;
    case HINT:
      hints.add(problem);
      break;
    }
  }

  void addAll(Problem[] problems) {
    for (Problem problem : problems) {
      add(problem);
    }
  }

  void addAll(GrammarProblems other) {
    problems.addAll(other.problems);
    fatals.addAll(other.fatals);
    errors.addAll(other.errors);
    warnings.addAll(other.warnings);
    hints.addAll(other.hints);
  }

  void sort() {
    Collections.sort(problems);
    Collections.sort(fatals);
    Collections.sort(errors);
    Collections.sort(warnings);
    Collections.sort(hints);
  }

  void clear() {
    for (Problem problem : problems) {
      if (problem.getRule() != null) {
        problem.getRule().problems.clear();
      }
    }

    problems.clear();
    fatals.clear();
    errors.clear();
    warnings.clear();
    hints.clear();
  }

  public boolean hasProblems() {
    return problems.size() > 0;
  }

  public List<Problem> getProblems() {
    return Collections.unmodifiableList(problems);
  }

  public boolean hasFatals() {
    return fatals.size() > 0;
  }

  public List<Problem> getFatals() {
    return Collections.unmodifiableList(fatals);
  }

  public boolean hasErrors() {
    return errors.size() > 0;
  }

  public List<Problem> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  public boolean hasWarnings() {
    return warnings.size() > 0;
  }

  public List<Problem> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  public boolean hasHints() {
    return hints.size() > 0;
  }

  public List<Problem> getHints() {
    return Collections.unmodifiableList(hints);
  }

  @Override
  public String getMessage() {
    return toString();
  }

  @Override
  public String toString() {
    return toString(new CharBuffer()).toString();
  }

  @Override
  public CharBuffer toString(CharBuffer buf) {
    if (problems.size() > 0) {
      for (Problem problem : problems) {
        problem.toString(buf);
        buf.append("\n");
      }

      toCountString(buf, Problem.Type.FATAL, fatals.size());
      toCountString(buf, Problem.Type.ERROR, errors.size());
      toCountString(buf, Problem.Type.WARNING, warnings.size());
      toCountString(buf, Problem.Type.HINT, hints.size());
    }
    return buf;
  }

  private static void toCountString(CharBuffer buf, Problem.Type type, int count) {
    if (count > 0) {
      buf.append(count).append(" ").append(type.name().toLowerCase());
      if (count > 1) {
        buf.append("s");
      }
      buf.append("\n");
    }
  }

}
