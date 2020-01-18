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

package org.foxlabs.peg4j;

import java.util.HashMap;

import java.net.URL;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.IOException;

import org.foxlabs.peg4j.grammar.Grammar;
import org.foxlabs.peg4j.grammar.ParseContext;
import org.foxlabs.peg4j.debug.RuleTracer;
import org.foxlabs.peg4j.debug.ErrorTracer;

import org.foxlabs.util.Location;

/**
 * Abstract base class for parsers.
 * 
 * <p>This class provides basic interface and methods required for parsing of
 * character stream. Subclasses should override {@link Parser#getGrammar()},
 * {@link Parser#getTransaction()} and {@link Parser#buildResult()} methods.
 * Memoization feature is supported and can be enabled or disabled using the
 * {@link Parser#setMemoable(boolean)} method. Also if there is a need to keep
 * track of parsing process then necessary rule tracer can be attached using the
 * {@link Parser#setTracer(RuleTracer)} method. A number of convenient
 * <code>parse()</code> methods are implemented to allow parsing from different
 * sources like string, URL, file, etc.</p>
 * 
 * @param <T> The type of semantic result for the parser.
 * 
 * @author Fox Mulder
 * @see DefaultParser
 * @see Grammar
 * @see Transaction
 * @see RuleTracer
 */
public abstract class Parser<T> {

  /**
   * Tracer that will be used by this parser to keep track of parsing process.
   * By default no tracer is configured.
   */
  private RuleTracer tracer = null;

  /**
   * This flag is used to enable or disable memoization feature for this parser.
   * By default memoization is enabled.
   */
  private boolean memoable = true;

  // Interface

  /**
   * Returns grammar for this parser.
   * 
   * <p>Grammar can be created using {@link org.foxlabs.peg4j.grammar.GrammarBuilder}
   * or {@link org.foxlabs.peg4j.grammar.GrammarParser}. Also you can create your
   * parser from the {@link DefaultParser} class.</p>
   * 
   * @return Grammar for this parser.
   * @see Grammar
   */
  protected abstract Grammar getGrammar();

  /**
   * Returns current transaction.
   * 
   * <p>According to grammar rules parser may execute semantic actions and each
   * action should be executed in separate transaction. Parser can invoke this
   * method many times during recognition process when transactional behavior is
   * required.</p>
   * 
   * @return Current transaction.
   * @see Transaction
   */
  protected abstract Transaction getTransaction();

  /**
   * Constructs and returns semantic result of the latest parsing.
   * 
   * <p>This method will be invoked by the {@link #parse(BacktrackingReader)}
   * method when parser successfully recognized input character stream.</p>
   * 
   * @return Semantic result of the latest parsing.
   */
  protected abstract T buildResult();

  // Configuration

  /**
   * Returns tracer that will be used by this parser to keep track of parsing
   * process.
   * 
   * @return Tracer that will be used by this parser to keep track of parsing
   *         process.
   */
  public final RuleTracer getTracer() {
    return tracer;
  }

  /**
   * Sets tracer that will be used by this parser to keep track of parsing
   * process.
   * 
   * @param tracer Tracer that will be used by this parser to keep track of
   *        parsing process.
   */
  public final void setTracer(RuleTracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Determines whether memoization feature is enabled for this parser.
   * 
   * @return <code>true</code> if memoization feature is enabled;
   *         <code>false</code> otherwise.
   */
  public final boolean isMemoable() {
    return memoable;
  }

  /**
   * Enables or disables memoization feature for this parser.
   * 
   * @param memoable Determines whether memoization feature should be enabled.
   */
  public final void setMemoable(boolean memoable) {
    this.memoable = memoable;
  }

  // Parsing

  /**
   * Parses the specified string and returns semantic result.
   * 
   * @param text String to be parsed.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize the specified
   *         string.
   */
  public final T parse(String text) throws IOException, RecognitionException {
    return parse(new BacktrackingReader(new StringReader(text)));
  }

  /**
   * Parses content retreived from the specified URL and returns semantic
   * result.
   * 
   * @param url URL which content to be parsed.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize content retreived
   *         from the specified URL.
   */
  public final T parse(URL url) throws IOException, RecognitionException {
    try (final InputStream stream = url.openStream()) {
      return parse(new BacktrackingReader(new InputStreamReader(stream), url.toString()));
    }
  }

  /**
   * Parses content retreived from the specified URL and returns semantic
   * result.
   * 
   * @param url URL which content to be parsed.
   * @param encoding Content encoding.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize content retreived
   *         from the specified URL.
   */
  public final T parse(URL url, String encoding) throws IOException, RecognitionException {
    try (final InputStream stream = url.openStream()) {
      return parse(new BacktrackingReader(new InputStreamReader(stream, encoding), url.toString()));
    }
  }

  /**
   * Parses content retreived from the specified file and returns semantic
   * result.
   * 
   * @param file File which content to be parsed.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize content retreived
   *         from the specified file.
   */
  public final T parse(File file) throws IOException, RecognitionException {
    try (final FileInputStream stream = new FileInputStream(file)) {
      return parse(new BacktrackingReader(new InputStreamReader(stream), file.getPath()));
    }
  }

  /**
   * Parses content retreived from the specified file and returns semantic
   * result.
   * 
   * @param file File which content to be parsed.
   * @param encoding Content encoding.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize content retreived
   *         from the specified file.
   */
  public final T parse(File file, String encoding) throws IOException, RecognitionException {
    try (FileInputStream stream = new FileInputStream(file)) {
      return parse(new BacktrackingReader(new InputStreamReader(stream, encoding), file.getPath()));
    }
  }

  /**
   * Parses the specified input stream and returns semantic result.
   * 
   * @param stream Input stream to be parsed.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize the specified input
   *         stream.
   */
  public final T parse(InputStream stream) throws IOException, RecognitionException {
    return parse(new BacktrackingReader(new InputStreamReader(stream)));
  }

  /**
   * Parses the specified input stream and returns semantic result.
   * 
   * @param stream Input stream to be parsed.
   * @param encoding Content encoding.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize the specified input
   *         stream.
   */
  public final T parse(InputStream stream, String encoding) throws IOException, RecognitionException {
    return parse(new BacktrackingReader(new InputStreamReader(stream, encoding)));
  }

  /**
   * Parses the specified input character stream and returns semantic result.
   * 
   * @param stream Input character stream to be parsed.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize the specified input
   *         character stream.
   */
  public final T parse(Reader stream) throws IOException, RecognitionException {
    if (stream instanceof BacktrackingReader) {
      return parse((BacktrackingReader) stream);
    } else {
      return parse(new BacktrackingReader(stream));
    }
  }

  /**
   * Parses the specified input character stream and returns semantic result.
   * 
   * @param stream Input character stream to be parsed.
   * @return Semantic result of parsing.
   * @throws IOException if IO error occurred.
   * @throws RecognitionException if parser cannot recognize the specified input
   *         character stream.
   */
  public final T parse(BacktrackingReader stream) throws IOException, RecognitionException {
    boolean success = false;
    ErrorTracer tracer = ErrorTracer.newTracer(getTracer());
    Context context = isMemoable() ? new MemoContext(stream, tracer) : new Context(stream, tracer);
    tracer.open(stream);
    try {
      success = getGrammar().getStart().reduce(context);
      if (!success) {
        throw tracer.newSyntaxException();
      }
      return buildResult();
    } finally {
      tracer.close(success);
    }
  }

  // Context

  /**
   * Default parsing context that does not support memoization feature.
   * 
   * @author Fox Mulder
   */
  private class Context extends Transaction.Adapter implements ParseContext {

    /**
     * Input character stream with backtrace feature.
     */
    final BacktrackingReader stream;

    /**
     * Tracer that keeps track of syntax errors.
     */
    final ErrorTracer tracer;

    /**
     * Constructs new context.
     * 
     * @param stream Input character stream with backtrace feature.
     * @param tracer Tracer that keeps track of syntax errors.
     */
    private Context(BacktrackingReader stream, ErrorTracer tracer) {
      this.stream = stream;
      this.tracer = tracer;
    }

    // ParseContext

    /**
     * Returns this parser instance.
     * 
     * @return This parser instance.
     * @see ParseContext#parser()
     */
    @Override
    public Parser<?> parser() {
      return Parser.this;
    }

    /**
     * Returns input character stream with backtrace feature.
     * 
     * @return Input character stream with backtrace feature.
     * @see ParseContext#stream()
     */
    @Override
    public BacktrackingReader stream() {
      return stream;
    }

    /**
     * Returns tracer that keeps track of parsing process.
     * 
     * @return Tracer that keeps track of parsing process.
     * @see ParseContext#tracer()
     */
    @Override
    public RuleTracer tracer() {
      return tracer;
    }

    /**
     * Returns this context instance.
     * 
     * @return This context instance.
     * @see ParseContext#transaction()
     */
    @Override
    public Transaction transaction() {
      return this;
    }

    // ActionContext

    /**
     * Returns number of parsed characters.
     * 
     * @return Number of parsed characters.
     * @see ActionContext#length()
     */
    @Override
    public int length() {
      return stream.getLength();
    }

    /**
     * Returns parsed characters as an array.
     * 
     * @return Parsed characters as an array.
     * @see ActionContext#chars()
     */
    @Override
    public char[] chars() {
      return stream.getChars();
    }

    /**
     * Returns parsed characters as a string.
     * 
     * @return Parsed characters as a string.
     * @see ActionContext#text()
     */
    @Override
    public String text() {
      return stream.getText();
    }

    /**
     * Returns start location in the input character stream.
     * 
     * @return Start location in the input character stream.
     * @see ActionContext#start()
     */
    @Override
    public Location start() {
      return stream.getStart();
    }

    /**
     * Returns end location in the input character stream.
     * 
     * @return End location in the input character stream.
     * @see ActionContext#end()
     */
    @Override
    public Location end() {
      return stream.getEnd();
    }

    // Transaction

    /**
     * Starts a new transaction.
     * 
     * @see Transaction#begin()
     */
    @Override
    public void begin() {
      getTransaction().begin();
    }

    /**
     * Commits changes of current transaction.
     * 
     * @see Transaction#commit()
     */
    @Override
    public void commit() {
      getTransaction().commit();
    }

    /**
     * Rolls back changes of current transaction.
     * 
     * @see Transaction#rollback()
     */
    @Override
    public void rollback() {
      getTransaction().rollback();
    }

  }

  // MemoContext

  /**
   * Parsing context with memoization support.
   * 
   * @author Fox Mulder
   * @see Context
   */
  private class MemoContext extends Context {

    /**
     * Cache of transaction snapshots.
     */
    final HashMap<Long, TxSnapshot> snapshotCache = new HashMap<Long, TxSnapshot>();

    /**
     * Constructs new context with memoization support.
     * 
     * @param stream Input character stream with backtrace feature.
     * @param tracer Tracer that keeps track of syntax errors.
     */
    private MemoContext(BacktrackingReader stream, ErrorTracer tracer) {
      super(stream, tracer);
    }

    /**
     * Applies snapshot previously stored by the {@link #save()} method.
     * 
     * <p>Snapshot considered to be successfully applied if it was found in the
     * cache and transaction was successfully loaded or position in the input
     * character stream was changed.</p>
     * 
     * @return <code>true</code> if snapshot was successfully applied;
     *         <code>false</code> otherwise.
     * @see Transaction#load()
     */
    @Override
    public boolean load() {
      TxSnapshot snapshot = snapshotCache.get(snapshotID());
      if (snapshot != null) {
        if (!(snapshot.delta == null || snapshot.delta.load())) {
          return false;
        }
        if (snapshot.length > 0) {
          try {
            stream.skip(snapshot.length);
          } catch (IOException e) {
            // Should never happen
            throw new IllegalStateException(e);
          }
        }
      }
      return false;
    }

    /**
     * Returns snapshot of changes in the scope of the current transaction as
     * separate transaction instance.
     * 
     * <p>If current transaction does not support memoization feature or has no
     * changes and position in the input character stream is unchanged then
     * <code>null</code> will be returned. This method returns
     * {@link Transaction#STATELESS} if transaction does not support memoization
     * feature or has no changes but position in the input character stream has
     * been changed.</p>
     * 
     * @return Snapshot of changes in the scope of current transaction as
     *         separate transaction instance or <code>null</code> if there is
     *         nothing to save.
     * @see Transaction#save()
     */
    @Override
    public Transaction save() {
      int length = stream.getLength();
      Transaction delta = getTransaction().save();
      if (length > 0 || delta != null) {
        snapshotCache.put(snapshotID(), new TxSnapshot(delta, length));
        return delta == null ? Transaction.STATELESS : delta;
      }
      return null;
    }

    /**
     * Returns snapshot ID that is concatenation of current production index and
     * offest in the input character stream.
     * 
     * @return Snapshot ID that is concatenation of current production index and
     *         offest in the input character stream.
     */
    private Long snapshotID() {
      return ((long) tracer.getCurrentReference().getIndex() << 32) | stream.getStartOffset();
    }

  }

  // TxSnapshot

  /**
   * An entry in the shapshot cache. This entry stores transaction snapshot and
   * number of parsed characters.
   * 
   * @author Fox Mulder
   */
  private static final class TxSnapshot {

    /**
     * Transaction snapshot.
     */
    final Transaction delta;

    /**
     * Number of parsed characters.
     */
    final int length;

    /**
     * Constructs new snapshot entry.
     * 
     * @param delta Transaction snapshot.
     * @param length Number of parsed characters.
     */
    private TxSnapshot(Transaction delta, int length) {
      this.delta = delta;
      this.length = length;
    }

  }

}
