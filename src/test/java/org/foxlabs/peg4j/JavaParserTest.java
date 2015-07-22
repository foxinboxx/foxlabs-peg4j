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

import java.io.IOException;

import org.junit.Test;

/**
 * A simple test that parses Java source file.
 * 
 * @author Fox Mulder
 */
@GrammarDecl(ref = "classpath:java16.peg4j")
public class JavaParserTest extends DefaultParser<Object> {
    
    private static final String SOURCE_PATH = "ArrayList.java";
    
    private String source = null;
    
    public void handleSource(ActionContext context) {
        source = context.text();
    }
    
    @Override
    protected Transaction getTransaction() {
        return Transaction.STATELESS;
    }
    
    @Override
    protected Object buildResult() {
        return null;
    }
    
    @Test
    public void testJavaGrammar() throws IOException {
        try {
            parse(getClass().getClassLoader().getResource(SOURCE_PATH));
            System.out.println(source);
        } catch (RecognitionException e) {
            System.err.println(e.getMessage());
        }
    }
    
}
