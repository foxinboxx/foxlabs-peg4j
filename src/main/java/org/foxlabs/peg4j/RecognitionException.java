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
 * Thrown to indicate a problem in recognition of character stream.
 * 
 * @author Fox Mulder
 */
public class RecognitionException extends Exception {
    private static final long serialVersionUID = 5930964505891131908L;
    
    /**
     * Location of a problem in character stream.
     */
    private Location location;
    
    /**
     * Constructs a new recognition exception with the specified location.
     * 
     * @param location Location of a problem in character stream.
     */
    public RecognitionException(Location location) {
        super();
        this.location = location;
    }
    
    /**
     * Constructs a new recognition exception with the specified detail message
     * and location.
     * 
     * @param message Detail message.
     * @param location Location of a problem in character stream.
     */
    public RecognitionException(String message, Location location) {
        super(message);
        this.location = location;
    }
    
    /**
     * Constructs a new recognition exception with the specified detail message,
     * cause and location.
     * 
     * @param message Detail message.
     * @param cause Problem cause.
     * @param location Location of a problem in character stream.
     */
    public RecognitionException(String message, Throwable cause, Location location) {
        super(message, cause);
        this.location = location;
    }
    
    /**
     * Constructs a new recognition exception with the specified cause and
     * location.
     * 
     * @param cause Problem cause.
     * @param location Location of a problem in character stream.
     */
    public RecognitionException(Throwable cause, Location location) {
        super(cause);
        this.location = location;
    }
    
    /**
     * Returns location of a problem in character stream.
     * 
     * @return Location of a problem in character stream.
     */
    public Location getLocation() {
        return location;
    }
    
    /**
     * Returns detail message.
     * 
     * @return Detail message.
     */
    public String getMessage() {
        return location.isUnknown() ? super.getMessage() : location + ": " + super.getMessage();
    }
    
}
