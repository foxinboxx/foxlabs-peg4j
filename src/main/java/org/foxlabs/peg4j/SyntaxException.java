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

import java.util.Set;

import org.foxlabs.peg4j.grammar.Terminal;
import org.foxlabs.peg4j.resource.ResourceManager;

import org.foxlabs.util.Location;

/**
 * Thrown to indicate syntax error in recognition of character stream.
 * 
 * @author Fox Mulder
 */
public class SyntaxException extends RecognitionException {
  private static final long serialVersionUID = -7679475223946902663L;

  /**
   * Constructs a new syntax exception with the specified location.
   * 
   * @param location Location of syntax error in character stream.
   */
  public SyntaxException(Location location) {
    super(ResourceManager.formatRuntimeMessage("runtime.syntaxError"),
        location);
  }

  /**
   * Constructs a new syntax exception with the specified set of expected
   * terminals and location.
   * 
   * @param expectedSet Set of expected terminals at error location.
   * @param location Location of syntax error in character stream.
   */
  public SyntaxException(Set<Terminal> expectedSet, Location location) {
    super(ResourceManager.formatRuntimeMessage("runtime.expectedTokens", expectedSet),
        location);
  }

}
