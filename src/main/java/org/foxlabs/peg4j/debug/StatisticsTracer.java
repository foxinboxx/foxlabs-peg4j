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

import java.io.IOException;

import org.foxlabs.peg4j.BacktrackingReader;
import org.foxlabs.peg4j.grammar.Reference;
import org.foxlabs.peg4j.grammar.Rule;

/**
 * 
 * @author Fox Mulder
 */
public class StatisticsTracer implements RuleTracer {
    
    @Override
    public void open(BacktrackingReader stream) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onTrace(Rule rule) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onBacktrace(Rule rule, boolean success) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onLookup(Reference reference, boolean hit) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onCache(Reference reference) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void close(boolean result) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
}
