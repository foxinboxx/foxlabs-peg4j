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

import org.foxlabs.peg4j.grammar.Action;
import org.foxlabs.peg4j.resource.ResourceManager;

import org.foxlabs.util.Location;

/**
 * Wrapper for exceptions thrown by semantic actions during recognition of
 * input character stream.
 * 
 * @author Fox Mulder
 */
public class ActionException extends RecognitionException {
    private static final long serialVersionUID = -914099915000589698L;
    
    /**
     * Constructs a new <code>ActionException</code> with the specified action,
     * cause and location in input character stream.
     * 
     * @param action Semantic action.
     * @param cause Exception thrown by semantic action.
     * @param location Error location in input character stream.
     */
    public ActionException(Action action, Throwable cause, Location location) {
        super(ResourceManager.formatRuntimeMessage("runtime.actionError", action.getName()), cause, location);
    }
    
}
