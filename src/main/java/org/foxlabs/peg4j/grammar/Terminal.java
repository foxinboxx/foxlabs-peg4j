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

import java.io.IOException;

import org.foxlabs.common.text.CharBuffer;
import org.foxlabs.common.text.CharEncoder;
import org.foxlabs.util.UnicodeSet;

import org.foxlabs.peg4j.BacktrackingReader;

public abstract class Terminal extends Expression {

  /**
   * Constructs a new terminal.
   *
   * @param owner Owner production.
   */
  private Terminal(Production owner) {
    super(owner);
  }

  /**
   * Determines whether or not this terminal matches any characters. All valid
   * terminals (except <code>Terminal.Nil</code>) must match one or more
   * characters.
   *
   * @return <code>true</code> if this terminal doesn't match any characters;
   *         <code>false</code> otherwise;
   */
  public abstract boolean isEmpty();

  /**
   * Determines whether or not this terminal can match only one concrete
   * character. Only the <code>Terminal.Token</code> can match more than one
   * character.
   *
   * @return <code>true</code> if this terminal can match only one concrete
   *         character; <code>false</code> otherwise.
   */
  public abstract boolean isDetermined();

  /**
   * Determines whether or not this terminal can be replaced by another
   * terminal, that is more efficient to match character stream. For example,
   * <code>'a'-'a'</code> can be replaced by <code>'a'</code>, that is more
   * efficient to match character 'a'.
   *
   * @return <code>true</code> if this terminal is inefficient;
   *         <code>false</code> otherwise.
   */
  public abstract boolean isInefficient();

  @Override
  public boolean reduce(ParseContext context) throws IOException {
    context.tracer().onRuleTrace(this);
    if (match(context.stream())) {
      context.tracer().onRuleBacktrace(this, true);
      return true;
    }
    context.tracer().onRuleBacktrace(this, false);
    return false;
  }

  /**
   * Attempts to match the given character stream.
   *
   * @param stream Character stream.
   * @return <code>true</code> if this terminal matches character stream;
   *         <code>false</code> otherwise.
   */
  protected abstract boolean match(BacktrackingReader stream) throws IOException;

  @Override
  public final <E extends Throwable> void accept(RuleVisitor<E> visitor) throws E {
    visitor.visit(this);
  }

  // Nil

  public static final class Nil extends Terminal {

    private Nil(Production owner) {
      super(owner);
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean isDetermined() {
      return false;
    }

    @Override
    public boolean isInefficient() {
      return false;
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      return true;
    }

    @Override
    public String toString() {
      return "";
    }

    @Override
    public String toString(boolean debug) {
      return "";
    }

    @Override
    public CharBuffer toString(CharBuffer buf) {
      return buf;
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      return buf;
    }

  }

  static Nil nil(Production owner) {
    return new Nil(owner);
  }

  // Any

  public static final class Any extends Terminal {

    private Any(Production owner) {
      super(owner);
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public boolean isDetermined() {
      return false;
    }

    @Override
    public boolean isInefficient() {
      return false;
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      return stream.read() >= 0;
    }

    @Override
    public String toString() {
      return ".";
    }

    @Override
    public String toString(boolean debug) {
      return ".";
    }

    @Override
    public CharBuffer toString(CharBuffer buf) {
      return buf.append('.');
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      return buf.append('.');
    }

  }

  static Any any(Production owner) {
    return new Any(owner);
  }

  // Token

  public static abstract class Token extends Terminal {

    private final String image;

    private Token(Production owner, String image) {
      super(owner);
      this.image = image;
    }

    public final String getImage() {
      return image;
    }

    @Override
    public boolean isEmpty() {
      return image.length() == 0;
    }

    public abstract boolean isCaseSensitive();

    @Override
    public int hashCode() {
      return image.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Token) {
        Token other = (Token) obj;
        if (isCaseSensitive() == other.isCaseSensitive()) {
          if (isCaseSensitive()) {
            return image.equals(other.image);
          } else {
            return image.equalsIgnoreCase(other.image);
          }
        }
      }
      return false;
    }

  }

  private static abstract class TokenCS extends Token {

    private TokenCS(Production owner, String image) {
      super(owner, image);
    }

    @Override
    public final boolean isInefficient() {
      return false;
    }

    @Override
    public final boolean isCaseSensitive() {
      return true;
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      return buf.append('\'').appendEncoded(getImage(), CharEncoder.JAVA).append('\'');
    }

  }

  private static abstract class TokenIC extends Token {

    private TokenIC(Production owner, String image) {
      super(owner, image);
    }

    @Override
    public final boolean isInefficient() {
      return getImage().toLowerCase().equals(getImage().toUpperCase());
    }

    @Override
    public final boolean isCaseSensitive() {
      return false;
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      return buf.append('\"').appendEncoded(getImage(), CharEncoder.JAVA).append('\"');
    }

  }

  private static final class AtomCS extends TokenCS {

    private final int value;

    private AtomCS(Production owner, char image) {
      super(owner, Character.toString(image));
      value = image;
    }

    @Override
    public boolean isDetermined() {
      return true;
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      return stream.read() == value;
    }

  }

  private static final class AtomIC extends TokenIC {

    private final int value;

    private AtomIC(Production owner, char image) {
      super(owner, Character.toString(image));
      value = Character.toUpperCase(image);
    }

    @Override
    public boolean isDetermined() {
      return false;
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      return Character.toUpperCase(stream.read()) == value;
    }

  }

  private static final class SequenceCS extends TokenCS {

    private final int[] value;

    private SequenceCS(Production owner, String image) {
      super(owner, image);
      value = new int[image.length()];
      for (int i = 0; i < value.length; i++) {
        value[i] = image.charAt(i);
      }
    }

    @Override
    public boolean isDetermined() {
      return false;
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      for (int i = 0; i < value.length; i++) {
        if (stream.read() != value[i]) {
          return false;
        }
      }
      return true;
    }

  }

  private static final class SequenceIC extends TokenIC {

    private final int[] value;

    private SequenceIC(Production owner, String image) {
      super(owner, image);
      value = new int[image.length()];
      for (int i = 0; i < value.length; i++) {
        value[i] = Character.toUpperCase(image.charAt(i));
      }
    }

    @Override
    public boolean isDetermined() {
      return false;
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      for (int i = 0; i < value.length; i++) {
        if (Character.toUpperCase(stream.read()) != value[i]) {
          return false;
        }
      }
      return true;
    }

  }

  static Token tokenOf(Production owner, char value, boolean cs) {
    return cs ? new AtomCS(owner, value) : new AtomIC(owner, value);
  }

  static Token tokenOf(Production owner, String value, boolean cs) {
    int length = value.length();
    if (length == 0) {
      return new SequenceCS(owner, value); // Empty sequence
    } else if (length == 1) {
      return tokenOf(owner, value.charAt(0), cs);
    } else {
      return cs ? new SequenceCS(owner, value) : new SequenceIC(owner, value);
    }
  }

  // Interval

  public static final class Interval extends Terminal {

    private final int min;

    private final int max;

    private Interval(Production owner, char min, char max) {
      super(owner);
      this.min = min;
      this.max = max;
    }

    public int getMin() {
      return min;
    }

    public int getMax() {
      return max;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public boolean isDetermined() {
      return true;
    }

    @Override
    public boolean isInefficient() {
      return min == max; // Can be replaced by Token
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      int ch = stream.read();
      return ch >= min && ch <= max;
    }

    @Override
    public int hashCode() {
      return min + max;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Interval) {
        Interval other = (Interval) obj;
        return min == other.min && max == other.max;
      }
      return false;
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      buf.append('\'');
      CharEncoder.JAVA.encode(min, buf);
      buf.append('\'').append('-').append('\'');
      CharEncoder.JAVA.encode(max, buf);
      buf.append('\'');
      return buf;
    }

  }

  static Interval intervalOf(Production owner, char min, char max) {
    return min < max ? new Interval(owner, min, max) : new Interval(owner, max, min);
  }

  // Set

  public static final class Set extends Terminal {

    private final UnicodeSet uset;

    private Set(Production owner, UnicodeSet uset) {
      super(owner);
      this.uset = uset;
    }

    public UnicodeSet getUnicodeSet() {
      return uset;
    }

    @Override
    public boolean isEmpty() {
      return uset == UnicodeSet.EMPTY;
    }

    @Override
    public boolean isDetermined() {
      return true;
    }

    @Override
    public boolean isInefficient() {
      return uset == UnicodeSet.WHOLE // Can be replaced by Any
          || uset.getMin() == uset.getMax() // Can be replaced by Token
          || uset.size() == 1; // Can be replaced by Interval
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      return uset.contains(stream.read());
    }

    @Override
    public int hashCode() {
      return uset.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Set) {
        Set other = (Set) obj;
        return uset.equals(other.uset);
      }
      return false;
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      return uset.toString(buf);
    }

  }

  static Set setOf(Production owner, char... values) {
    return new Set(owner, UnicodeSet.fromElements(values));
  }

  static Set setOf(Production owner, String values) {
    return setOf(owner, values.toCharArray());
  }

  static Set setOf(Production owner, UnicodeSet uset) {
    if (uset == null) {
      throw new IllegalArgumentException();
    } else {
      return new Set(owner, uset);
    }
  }

  static Set setOf(Production owner, UnicodeSet... usets) {
    return new Set(owner, UnicodeSet.unionAll(usets));
  }

  // Class

  public static class Class extends Terminal implements Cloneable {

    private final String name;

    private Class(Production owner, String name) {
      super(owner);
      this.name = name;
    }

    public final String getName() {
      return name;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public boolean isDetermined() {
      return false;
    }

    @Override
    public boolean isInefficient() {
      return false;
    }

    public final boolean isUndefined() {
      return getClass() == Class.class;
    }

    @Override
    protected boolean match(BacktrackingReader stream) throws IOException {
      return match(stream.read());
    }

    protected boolean match(int ch) {
      return false;
    }

    @Override
    protected Class clone() {
      try {
        return (Class) super.clone();
      } catch (CloneNotSupportedException e) {
        // should never happen
        throw new InternalError();
      }
    }

    @Override
    public CharBuffer toString(CharBuffer buf, boolean debug) {
      return buf.append('<').append(name).append('>');
    }

  }

  // Lower

  private static final class Lower extends Class {

    public Lower(Production owner, String name) {
      super(owner, name);
    }

    @Override
    protected boolean match(int ch) {
      return Character.isLowerCase(ch);
    }

  }

  // Upper

  private static final class Upper extends Class {

    public Upper(Production owner, String name) {
      super(owner, name);
    }

    @Override
    protected boolean match(int ch) {
      return Character.isUpperCase(ch);
    }

  }

  // Title

  private static final class Title extends Class {

    public Title(Production owner, String name) {
      super(owner, name);
    }

    @Override
    protected boolean match(int ch) {
      return Character.isTitleCase(ch);
    }

  }

  // Alpha

  private static final class Alpha extends Class {

    public Alpha(Production owner, String name) {
      super(owner, name);
    }

    @Override
    protected boolean match(int ch) {
      return Character.isLetter(ch);
    }

  }

  // Digit

  private static final class Digit extends Class {

    public Digit(Production owner, String name) {
      super(owner, name);
    }

    @Override
    protected boolean match(int ch) {
      return Character.isDigit(ch);
    }

  }

  // Alnum

  private static final class Alnum extends Class {

    public Alnum(Production owner, String name) {
      super(owner, name);
    }

    @Override
    protected boolean match(int ch) {
      return Character.isLetterOrDigit(ch);
    }

  }

  // Space

  private static final class Space extends Class {

    public Space(Production owner, String name) {
      super(owner, name);
    }

    @Override
    protected boolean match(int ch) {
      return Character.isWhitespace(ch);
    }

  }

  private static final Class[] CLASSES = new Class[] { new Lower(null, "LOWER"), new Upper(null, "UPPER"),
      new Title(null, "TITLE"), new Alpha(null, "ALPHA"), new Digit(null, "DIGIT"), new Alnum(null, "ALNUM"),
      new Space(null, "SPACE") };

  /**
   * Returns all supported character class names.
   *
   * @return Array of the supported character class names.
   */
  public static String[] getClassNames() {
    String[] names = new String[CLASSES.length];
    for (int i = 0; i < CLASSES.length; i++) {
      names[i] = CLASSES[i].getName();
    }
    return names;
  }

  static Class classOf(Production owner, String name) {
    for (int i = 0; i < CLASSES.length; i++) {
      if (name.equalsIgnoreCase(CLASSES[i].getName())) {
        Class c = CLASSES[i].clone();
        c.owner = owner;
        return c;
      }
    }
    return new Class(owner, name);
  }

}
