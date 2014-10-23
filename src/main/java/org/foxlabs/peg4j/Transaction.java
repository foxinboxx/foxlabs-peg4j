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

/**
 * 
 * @author Fox Mulder
 */
public interface Transaction {
    
    void commit();
    
    void rollback();
    
    void store();
    
    void restore();
    
    Transaction NONE = new Transaction() {
        
        @Override
        public void commit() {
            // noop
        }
        
        @Override
        public void rollback() {
            // noop
        }
        
        @Override
        public void store() {
            // noop
        }
        
        @Override
        public void restore() {
            // noop
        }
        
    };
    
}
