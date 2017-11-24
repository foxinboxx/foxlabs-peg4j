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

import org.foxlabs.peg4j.grammar.GrammarBuilder;
import org.foxlabs.peg4j.grammar.GrammarParser;

/**
 * An action handler that should be implemented by parser and will be invoked
 * each time when specific semantic action takes place in recognition process.
 * 
 * <p> Each semantic action should be uniquely defined by name within grammar
 * and correspondng action handler should be associated with it. </p>
 * 
 * <p> Such bindings may be provided to constructor of the  {@link GrammarBuilder}
 * or {@link GrammarParser}. Also you can override {@link DefaultParser} that
 * creates those bindings automatically according to method signatures defined
 * on parser class. </p>
 * 
 * @author Fox Mulder
 * @see ActionContext
 * @see Parser
 * @see DefaultParser
 * @see GrammarBuilder
 * @see GrammarParser
 */
public interface ActionHandler<P extends Parser<?>> {
    
    /**
     * Handles specific semantic action and returns <code>true</code> if action
     * was successfully handled; <code>false</code> otherwise.
     * 
     * @param parser Parser instance.
     * @param context Action context.
     * @return <code>true</code> if semantic action was successfully handled;
     *         <code>false</code> otherwise.
     * @throws Throwable if error occurred and further recoginition of
     *         character stream is not possible.
     */
    boolean handle(P parser, ActionContext context) throws Throwable;
    
    /**
     * Action handler that does nothing.
     */
    ActionHandler<?> NOP = new ActionHandler<Parser<?>>() {
        public boolean handle(Parser<?> parser, ActionContext context) {
            return true;
        }
    };
    
}
