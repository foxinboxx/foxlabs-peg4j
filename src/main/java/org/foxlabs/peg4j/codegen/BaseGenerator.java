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

package org.foxlabs.peg4j.codegen;

import java.io.Writer;
import java.io.StringReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.foxlabs.peg4j.BacktrackingReader;
import org.foxlabs.peg4j.grammar.Grammar;
import org.foxlabs.peg4j.resource.ResourceManager;

public abstract class BaseGenerator {
    
    protected abstract String getTemplate();
    
    protected void defineVariables(Grammar grammar, Map<String, String> variables) {
        variables.put("product_name", ResourceManager.getProductName());
        variables.put("product_version", ResourceManager.getProductVersion());
        variables.put("product_url", ResourceManager.getProductURL());
    }
    
    public void generate(Grammar grammar, Writer out) throws IOException {
        Map<String, String> variables = new HashMap<String, String>();
        defineVariables(grammar, variables);
        generate(getTemplate(), variables, out);
    }
    
    protected static void generate(String template, Map<String, String> variables, Writer out) throws IOException {
        BacktrackingReader in = new BacktrackingReader(new StringReader(template));
        try {
            for (int ch = in.read(); ch >= 0; ch = in.read()) {
                if (ch == '$') {
                    ch = in.read();
                    if (ch == '{') {
                        int ident = in.getEndColumn() - 3;
                        StringBuilder var = new StringBuilder();
                        for (ch = in.read();; ch = in.read()) {
                            if (ch < 0) {
                                out.write('$');
                                out.write('{');
                                out.write(var.toString());
                                break;
                            } else if (ch == '}') {
                                String name = var.toString();
                                if (variables.containsKey(name)) {
                                    String value = variables.get(name);
                                    if (value == null || value.length() == 0) {
                                        break;
                                    }
                                    writeVariable(value, ident, out);
                                } else {
                                    out.write('$');
                                    out.write('{');
                                    out.write(name);
                                    out.write('}');
                                }
                                break;
                            } else {
                                var.append((char) ch);
                            }
                        }
                    } else {
                        out.write('$');
                        if (ch >= 0) {
                            out.write(ch);
                        }
                    }
                } else {
                    out.write(ch);
                }
            }
        } finally {
            in.close();
        }
    }
    
    protected static void writeVariable(String value, int ident, Writer out) throws IOException {
        if (value.endsWith("\n\r") || value.endsWith("\r\n")) {
            value = value.substring(0, value.length() - 2);
        } else if (value.endsWith("\n") || value.endsWith("\r")) {
            value = value.substring(0, value.length() - 1);
        }
        
        BacktrackingReader in = new BacktrackingReader(new StringReader(value));
        try {
            for (int ch = in.read(); ch >= 0; ch = in.read()) {
                if (ch == BacktrackingReader.EOL) {
                    out.write(LINE_SEPARATOR);
                    for (int i = 0; i < ident; i++) {
                        out.write(' ');
                    }
                } else {
                    out.write(ch);
                }
            }
        } finally {
            in.close();
        }
    }
    
    protected static void appendIdent(int ident, StringBuilder buf) {
        for (int i = 0; i < ident; i++) {
            buf.append(' ');
        }
    }
    
    static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
}
