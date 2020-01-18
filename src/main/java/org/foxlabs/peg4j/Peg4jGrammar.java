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

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.foxlabs.util.resource.ResourceHelper;

/**
 * This annotation allows to declare grammars for parsers either as source code
 * or as reference to a resource.
 * 
 * <p>
 * Source code:
 * 
 * <pre>
 * &#x0040;Peg4jGrammar(
 *   "Expr : Sum !. ;" +
 *   "Sum : Product (('+' / '-') Product)* ;" +
 *   "Product : Value (('*' / '/') Value)* ;" +
 *   "Value : '0'-'9'+ / '(' Expr ')' ;"
 * )
 * public class MyParser extends DefaultParser&lt;Integer&gt; {
 *   ...
 * }
 * </pre>
 * 
 * Reference to a classpath resource:
 * 
 * <pre>
 * &#x0040;Peg4jGrammar(ref = "classpath:expr.peg4j")
 * public class MyParser extends DefaultParser&lt;Integer&gt; {
 *   ...
 * }
 * </pre>
 * </p>
 * 
 * @author Fox Mulder
 * @see DefaultParser
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Peg4jGrammar {

  /**
   * Grammar source code.
   */
  String value() default "";

  /**
   * Reference to grammar resource. You may use
   * <code>classpath:path/to/grammar</code> if grammar resource is available at
   * classpath.
   */
  String ref() default "";

  /**
   * Grammar enconding. Default is UTF-8.
   */
  String encoding() default ResourceHelper.DEFAULT_ENCODING;

}
