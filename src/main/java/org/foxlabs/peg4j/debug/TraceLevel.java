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

package org.foxlabs.peg4j.debug;

import org.foxlabs.peg4j.grammar.Rule;
import org.foxlabs.peg4j.grammar.Action;
import org.foxlabs.peg4j.grammar.Reference;
import org.foxlabs.peg4j.grammar.Production;

public enum TraceLevel {

  HIGH, MEDIUM, LOW;

  public static TraceLevel forRule(Rule rule) {
    if (rule instanceof Production) {
      return HIGH;
    } else if (rule instanceof Reference || rule instanceof Action) {
      return MEDIUM;
    } else {
      return LOW;
    }
  }

}
