/* 
 * Copyright (C) 2015 FoxLabs
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

import java.util.HashMap;

public abstract class MemoableTransaction<S> implements Transaction {
    
    private final HashMap<Long, Entry<S>> snapshotCache = new HashMap<Long, Entry<S>>();
    
    protected abstract void load(S snapshot);
    
    protected abstract S save();
    
    @Override
    public final int load(long id) {
        Entry<S> entry = snapshotCache.get(id);
        if (entry != null) {
            load(entry.snapshot);
            return entry.length;
        }
        return -1;
    }
    
    @Override
    public final void save(long id, int length) {
        S snapshot = save();
        if (snapshot != null || length > 0) {
            snapshotCache.put(id, new Entry<S>(snapshot, length));
        }
    }
    
    public void clear() {
        snapshotCache.clear();
    }
    
    // Entry
    
    private static final class Entry<S> {
        
        private final S snapshot;
        private final int length;
        
        private Entry(S snapshot, int length) {
            this.snapshot = snapshot;
            this.length = length;
        }
        
    }
    
}
