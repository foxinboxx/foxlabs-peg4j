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
    
    /**
     * Starts a new transaction. Note that if this transaction was started and
     * not committed or rolled back then this method should start nested
     * transaction. When nested transaction will be committed changes of nested
     * transaction should become a part of this transaction. Depth of nested
     * transactions is not limited.
     */
    void begin();
    
    /**
     * Commits changes of this transaction. If this transaction is nested then
     * all the changes should became a part of parent transaction.
     */
    void commit();
    
    /**
     * Rolls back changes of this transaction.
     */
    void rollback();
    
    /**
     * Applies snapshot previously stored by the {@link #save()} method to the
     * current transaction. This method might be invoked only from parsers that
     * have memoable property set to <code>true</code>.
     * If transaction does not support memoization feature then this method
     * should return <code>false</code>. You can override {@link Adapter} class
     * that initially implemented with memoization feature turned off.
     * 
     * @return <code>true</code> if snapshot was successfully applied to the
     *         current transaction; <code>false</code> otherwise.
     */
    boolean load();
    
    /**
     * Returns snapshot of changes in the scope of the current transaction as
     * separate transaction instance. This method might be invoked only from
     * parsers that have memoable property set to <code>true</code>.
     * If transaction does not support memoization feature then this method
     * should return <code>null</code>. You can override {@link Adapter} class
     * that initially implemented with memoization feature turned off.
     * 
     * @return Snapshot of changes in the scope of current transaction as
     *         separate transaction instance or <code>null</code> if
     *         transaction does not support memoization feature.
     */
    Transaction save();
    
    /**
     * Static instance for stateless transaction (i.e. transaction that does
     * not store actions state). This instance can be useful when parser just
     * needs to validate character stream and no actions take place.
     */
    Transaction STATELESS = new Adapter();        
    
    // Adapter
    
    /**
     * Adapter class for transaction that does not store any state and does not
     * support memoization feature.
     * 
     * @author Fox Mulder
     */
    class Adapter implements Transaction {
        
        @Override
        public void begin() {
            // Do nothing
        }
        
        @Override
        public void commit() {
            // Do nothing
        }
        
        @Override
        public void rollback() {
            // Do nothing
        }
        
        @Override
        public boolean load() {
            // Memoization feature is not supported
            return false;
        }
        
        @Override
        public Transaction save() {
            // Memoization feature is not supported
            return null;
        }
        
    }
    
}
