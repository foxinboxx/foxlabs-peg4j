/* 
 * Copyright (C) 2015 FoxLabs
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

package org.foxlabs.peg4j.debug;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;

import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;

import org.foxlabs.peg4j.BacktrackingReader;
import org.foxlabs.peg4j.grammar.Rule;
import org.foxlabs.peg4j.grammar.Action;
import org.foxlabs.peg4j.grammar.Terminal;
import org.foxlabs.peg4j.grammar.Production;
import org.foxlabs.peg4j.grammar.Reference;

import org.foxlabs.util.counter.HitCounter;
import org.foxlabs.util.counter.HitLatencyCounter;

import static org.foxlabs.util.counter.Counters.*;

/**
 * 
 * @author Fox Mulder
 */
public class StatisticsTracer implements RuleTracer {

  protected final HitLatencyCounter totalRuleCounter = defaultHitLatencyCounter();

  protected final HitLatencyCounter totalTerminalCounter = defaultHitLatencyCounter();

  protected final HitLatencyCounter totalProductionCounter = defaultHitLatencyCounter();

  protected final HitLatencyCounter totalActionCounter = defaultHitLatencyCounter();

  protected final Map<String, HitLatencyCounter> terminalStatisticsTable =
      new LinkedHashMap<String, HitLatencyCounter>();

  protected final Map<String, HitLatencyCounter> productionStatisticsTable =
      new LinkedHashMap<String, HitLatencyCounter>();

  protected final Map<String, HitLatencyCounter> actionStatisticsTable =
      new LinkedHashMap<String, HitLatencyCounter>();

  protected final HitCounter totalMemoCounter = defaultHitCounter();

  protected final Map<Reference, HitCounter> memoStatisticsTable =
      new IdentityHashMap<Reference, HitCounter>();

  protected int memoSize = 0;

  @Override
  public void open(BacktrackingReader stream) throws IOException {
    // Does nothing
  }

  @Override
  public void onRuleTrace(Rule rule) throws IOException {
    totalRuleCounter.start();
    if (rule instanceof Terminal) {
      String key = rule.toString();
      HitLatencyCounter counter = terminalStatisticsTable.get(key);
      if (counter == null) {
        terminalStatisticsTable.put(key, counter = defaultHitLatencyCounter());
      }
      counter.start();
    } else if (rule instanceof Production) {
      String key = ((Production) rule).getName();
      HitLatencyCounter counter = productionStatisticsTable.get(key);
      if (counter == null) {
        productionStatisticsTable.put(key, counter = defaultHitLatencyCounter());
      }
      counter.start();
    }
  }

  @Override
  public void onRuleBacktrace(Rule rule, boolean success) throws IOException {
    totalRuleCounter.stop(success);
    if (rule instanceof Terminal) {
      String key = rule.toString();
      HitLatencyCounter counter = terminalStatisticsTable.get(key);
      counter.stop(success);
    } else if (rule instanceof Production) {
      String key = ((Production) rule).getName();
      HitLatencyCounter counter = productionStatisticsTable.get(key);
      counter.stop(success);
    }
  }

  @Override
  public void onBeforeAction(Action action) throws IOException {
    if (!action.isInjected()) {
      String key = action.getName();
      HitLatencyCounter counter = actionStatisticsTable.get(key);
      if (counter == null) {
        actionStatisticsTable.put(key, counter = defaultHitLatencyCounter());
      }
      counter.start();
    }
  }

  @Override
  public void onAfterAction(Action action, boolean success) throws IOException {
    if (!action.isInjected()) {
      String key = action.getName();
      HitLatencyCounter counter = actionStatisticsTable.get(key);
      counter.stop(success);
    }
  }

  @Override
  public void onCacheGet(Reference reference, boolean hit) throws IOException {
    totalMemoCounter.increment(hit);
    HitCounter counter = memoStatisticsTable.get(reference);
    if (counter == null) {
      memoStatisticsTable.put(reference, counter = defaultHitCounter());
    }
    counter.increment(hit);
  }

  @Override
  public void onCachePut(Reference reference) throws IOException {
    memoSize++;
  }

  @Override
  public void close(boolean result) throws IOException {
    // Calculate total terminal statistcs
    for (HitLatencyCounter counter : terminalStatisticsTable.values()) {
      totalTerminalCounter.merge(counter);
    }

    // Calculate total production statistcs
    for (HitLatencyCounter counter : productionStatisticsTable.values()) {
      totalProductionCounter.merge(counter);
    }

    // Calculate total action statistcs
    for (HitLatencyCounter counter : actionStatisticsTable.values()) {
      totalActionCounter.merge(counter);
    }
  }

  public void reset() {
    totalMemoCounter.reset();

    totalRuleCounter.reset();
    totalTerminalCounter.reset();
    totalProductionCounter.reset();
    totalActionCounter.reset();

    terminalStatisticsTable.clear();
    productionStatisticsTable.clear();
    actionStatisticsTable.clear();
    memoStatisticsTable.clear();
  }

  public void print() throws IOException {
    print(new OutputStreamWriter(System.out));
  }

  public void print(Writer out) throws IOException {
    StringBuilder terminalStatisticsBuf = new StringBuilder();
    StringBuilder productionStatisticsBuf = new StringBuilder();
    StringBuilder actionStatisticsBuf = new StringBuilder();
    StringBuilder memoStatisticsBuf = new StringBuilder();

    for (Map.Entry<String, HitLatencyCounter> entry : terminalStatisticsTable.entrySet()) {
      terminalStatisticsBuf.append(entry.getKey()).append(" : ");
      entry.getValue().toString(terminalStatisticsBuf).append("\n");
    }
    for (Map.Entry<String, HitLatencyCounter> entry : productionStatisticsTable.entrySet()) {
      productionStatisticsBuf.append(entry.getKey()).append(" : ");
      entry.getValue().toString(productionStatisticsBuf).append("\n");
    }
    for (Map.Entry<String, HitLatencyCounter> entry : actionStatisticsTable.entrySet()) {
      actionStatisticsBuf.append(entry.getKey()).append(" : ");
      entry.getValue().toString(actionStatisticsBuf).append("\n");
    }
    for (Map.Entry<Reference, HitCounter> entry : memoStatisticsTable.entrySet()) {
      memoStatisticsBuf.append(entry.getKey().getTargetName()).append(" (");
      entry.getKey().getStart().toString(memoStatisticsBuf).append(") : ");
      entry.getValue().toString(memoStatisticsBuf).append("\n");
    }

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("totalRuleStatistics", totalRuleCounter);
    variables.put("totalTerminalStatistics", totalTerminalCounter);
    variables.put("totalProductionStatistics", totalProductionCounter);
    variables.put("totalActionStatistics", totalActionCounter);
    variables.put("terminalStatisticsTable", terminalStatisticsBuf);
    variables.put("productionStatisticsTable", productionStatisticsBuf);
    variables.put("actionStatisticsTable", actionStatisticsBuf);
    variables.put("totalMemoStatistics", totalMemoCounter);
    variables.put("memoStatisticsTable", memoStatisticsBuf);
  }

}
