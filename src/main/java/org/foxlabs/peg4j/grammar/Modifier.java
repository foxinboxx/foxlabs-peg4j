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

public enum Modifier {
    
    MEMO("@");
    
    private final String symbol;
    
    private Modifier(String symbol) {
        this.symbol = symbol;
    }
    
    public String toString() {
        return symbol;
    }
    
    public static Modifier modifierOf(String symbol) {
        return MEMO.symbol.equals(symbol) ? MEMO : null;
    }
    
}
