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

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.Assert;

import org.foxlabs.util.counter.Counters;
import org.foxlabs.util.counter.LatencyCounter;
import org.foxlabs.util.counter.HitLatencyCounter;

/**
 * Java parser performance test.
 * 
 * @author Fox Mulder
 */
@Peg4jGrammar(ref = "classpath:java18.peg4j")
public class JavaParserPerformanceTest extends DefaultParser<Object> {

  /**
   * Returns {@link Transaction#STATELESS}.
   * 
   * @return {@link Transaction#STATELESS}.
   */
  @Override
  protected Transaction getTransaction() {
    return Transaction.STATELESS;
  }

  /**
   * Returns <code>null</code>.
   * 
   * @return <code>null</code>.
   */
  @Override
  protected Object buildResult() {
    return null;
  }

  /**
   * Parses JDK sources and prints statistics.
   */
  @Test
  public void testJavaParser() throws IOException {
    // JDK sources are not loaded yet
    // Check that src.zip file exists
    Assert.assertTrue("Environment variable JAVA_HOME should be set.", JAVA_HOME_DIR.isDirectory());
    Assert.assertTrue("JDK is not installed or sources are not included.", JAVA_SRC_FILE.isFile());

    // Read all zip entries and parse Java sources
    final StringBuilder failures = new StringBuilder();
    final HitLatencyCounter totalCounter = Counters.defaultHitLatencyCounter();
    final LatencyCounter counter = Counters.defaultLatencyCounter();
    try (final ZipFile zipFile = new ZipFile(JAVA_SRC_FILE)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory() && entry.getName().endsWith(".java")) {
          System.out.print(entry.getName());
          RecognitionException failure = null;
          counter.reset();
          totalCounter.start();
          counter.start();
          try {
            parse(zipFile.getInputStream(entry));
          } catch (RecognitionException e) {
            failure = e;
          } finally {
            counter.stop();
            totalCounter.stop(true);
          }
          System.out.println(": " + (failure == null ? "SUCCESS" : "FAILURE") +
              " " + Counters.formatLatency(counter.getTotalLatency()) + "s");
          if (failure != null) {
            failures.append(entry.getName()).append(": ").append(failure.getMessage()).append("\n");
          }
        }
      }
    }

    // Print total results
    System.out.println("\nPARSE RESULTS: " + totalCounter);
    if (failures.length() > 0) {
      System.out.println("\nFAILURES:\n" + failures);
    }
  }

  // JAVA_HOME directory
  private static final File JAVA_HOME_DIR = new File(System.getenv("JAVA_HOME"));

  // JAVA_HOME/src.zip file
  private static final File JAVA_SRC_FILE = new File(JAVA_HOME_DIR, "src.zip");

}
