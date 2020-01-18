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

import org.foxlabs.util.Location;

/**
 * Describes context that will be passed on the
 * {@link ActionHandler#handle(Parser, ActionContext)} method when parser needs
 * to execute semantic action according to grammar rules.
 * 
 * @author Fox Mulder
 * @see ActionHandler
 */
public interface ActionContext {

  /**
   * Returns number of parsed characters.
   * 
   * @return Number of parsed characters.
   */
  int length();

  /**
   * Returns parsed characters as an array.
   * 
   * @return Parsed characters as an array.
   */
  char[] chars();

  /**
   * Returns parsed characters as a string.
   * 
   * @return Parsed characters as a string.
   */
  String text();

  /**
   * Returns start location in the input character stream.
   * 
   * @return Start location in the input character stream.
   */
  Location start();

  /**
   * Returns end location in the input character stream.
   * 
   * @return End location in the input character stream.
   */
  Location end();

}
