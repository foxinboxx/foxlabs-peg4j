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

import org.foxlabs.peg4j.ActionException;
import org.foxlabs.peg4j.BacktrackingReader;

/**
 * 
 * @author Fox Mulder
 */
public interface ParseContext {
    
    // Stream operations
    
    BacktrackingReader getStream();
    
    // State operations
    
    void startTransaction();
    
    void commitTransaction();
    
    void rollbackTransaction();
    
    void storeTransaction(Production target) throws IOException;
    
    boolean restoreTransaction(Production target) throws IOException;
    
    boolean executeAction(Action action) throws ActionException;
    
    // Trace operations
    
    void traceRule(Rule rule) throws IOException;
    
    void backtraceRule(Rule rule, boolean success) throws IOException;
    
}
