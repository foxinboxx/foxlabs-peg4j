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

import java.net.URL;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.IOException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.foxlabs.peg4j.grammar.Grammar;
import org.foxlabs.peg4j.grammar.GrammarParser;

import org.foxlabs.util.reflect.MethodInvoker;
import org.foxlabs.util.resource.ResourceHelper;

/**
 * 
 * 
 * @author Fox Mulder
 * @see Parser
 * @see Grammar
 */
public abstract class DefaultParser<T> extends Parser<T> {
    
    /**
     * Cache for parser grammars.
     */
    private static final ConcurrentMap<Class<?>, Grammar> grammarCache =
            new ConcurrentHashMap<Class<?>, Grammar>();
    
    /**
     * Grammar for this parser.
     */
    private final Grammar grammar;
    
    /**
     * Constructs a new parser and initializes its grammar.
     * 
     * @see #getGrammar()
     */
    public DefaultParser() {
        this.grammar = getGrammar();
    }
    
    /**
     * Returns grammar for this parser. If grammar for this parser class
     * was already loaded then it will be returned from the cache, the
     * {@link #loadGrammar()} method will be invoked otherwise.
     * 
     * @return Grammar for this parser.
     * @throws Peg4jException if an error occurs while loading the grammar.
     * @see #loadGrammar()
     */
    @Override
    public final Grammar getGrammar() {
        if (grammar == null) {
            Grammar grammar = grammarCache.get(getClass());
            if (grammar == null) {
                grammar = loadGrammar();
                grammarCache.put(getClass(), grammar);
            }
            return grammar;
        }
        return grammar;
    }
    
    /**
     * Loads grammar for this parser. The {@link #getGrammarStream()} method
     * is used to load grammar source and the {@link #getActionBindings()}
     * method provides action bindings.
     * 
     * <p> This method could be overriden in subclasses to provide another logic
     * for grammar loading. </p>
     * 
     * @return Grammar for this parser.
     * @throws Peg4jException if an error occurs while loading the grammar.
     * @see #getGrammarStream()
     * @see #getActionBindings()
     */
    protected Grammar loadGrammar() {
        BacktrackingReader stream = null;
        try {
            stream = getGrammarStream();
            if (stream == null) {
                throw new Peg4jException("Can't find grammar for parser " +
                        getClass().getName());
            }
            
            GrammarParser parser = new GrammarParser(getActionBindings());
            Grammar grammar = parser.parse(stream);
            if (grammar.hasErrors()) {
                throw new Peg4jException("Error compiling grammar for parser " +
                        getClass().getName() + ":\n" + grammar.getProblems());
            }
            
            return grammar;
        } catch (IOException e) {
            throw new Peg4jException("IO error occured when reading " +
                    getClass().getName() + " parser grammar", e);
        } catch (RecognitionException e) {
            throw new Peg4jException("Error occured when building " +
                    getClass().getName() + " parser grammar", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore it   
                }
            }
        }
    }
    
    /**
     * Returns {@link BacktrackingReader} character stream initialized to load
     * grammar source. This method looks up for the {@link GrammarDecl}
     * annotation first. If annotation is not present then it will try to find
     * classpath resource in the same package and with the same name as this
     * parser class and with <code>.peg4j</code> extension. If this class has
     * no annotation and there is no such resource on the classpath then
     * algorithm will be repeated for its subclasses.
     * 
     * @return {@link BacktrackingReader} character stream initialized to load
     *         grammar source or <code>null</code> if grammar source is not
     *         found.
     * @throws IOException if IO error occurs.
     */
    protected BacktrackingReader getGrammarStream() throws IOException {
        final ClassLoader cl = ResourceHelper.getClassLoader();
        
        for (Class<?> c = getClass(); c != DefaultParser.class; c = c.getSuperclass()) {
            // Lookup for annotation
            GrammarDecl decl = c.getAnnotation(GrammarDecl.class);
            if (decl != null) {
                if (decl.value().trim().length() > 0) {
                    return new BacktrackingReader(new StringReader(decl.value()), c.getName());
                }
                
                InputStream stream = decl.ref().toLowerCase().startsWith("classpath:")
                    ? cl.getResourceAsStream(decl.ref().substring(10))
                    : new URL(decl.ref()).openStream();
                if (stream == null) {
                    throw new IOException("Can't find grammar declaration: " +
                            decl.ref());
                }
                
                return new BacktrackingReader(new InputStreamReader(stream,
                        decl.encoding()), decl.ref());
            }
            
            // Lookup for classpath resource
            String file = ResourceHelper.getResourcePath(c) + "/" +
                    c.getSimpleName() + ".peg4j";
            InputStream stream = cl.getResourceAsStream(file);
            if (stream != null) {
                return new BacktrackingReader(new InputStreamReader(stream), file);
            }
        }
        
        // Found nothing
        return null;
    }
    
    /**
     * Returns action bindings as map where keys are action names and values
     * are corresponding action handlers. This method looks up through class
     * hierarchy all the methods for which the {@link #isActionMethod(Method)}
     * method returns <code>true</code>.
     * 
     * @return Action bindings as map where keys are action names and values
     *         are corresponding action handlers.
     * @see #isActionMethod(Method)
     * @see #createHandler(Method)
     */
    protected Map<String, ActionHandler<?>> getActionBindings() {
        Map<String, ActionHandler<?>> bindings = new HashMap<String, ActionHandler<?>>();
        
        for (Class<?> c = getClass(); c != DefaultParser.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (bindings.get(m.getName()) == null) {
                    if (isActionMethod(m)) {
                        bindings.put(m.getName(), createHandler(m));
                    }
                }
            }
        }
        
        return bindings;
    }
    
    /**
     * Determines if the specified method is an action method. Action method
     * cannot be abstract or static, must return void or boolean and must
     * accept one argument of the {@link ActionContext} type.
     * 
     * @param m Method to be checked.
     * @return <code>true</code> if the specified method is an action method;
     *         <code>false</code> otherwise.
     */
    protected boolean isActionMethod(Method m) {
        // Check modifiers
        if ((m.getModifiers() & (Modifier.ABSTRACT | Modifier.STATIC)) != 0) {
            return false;
        }
        
        // Check return type
        Class<?> type = m.getReturnType();
        if (!(type == Void.TYPE || type == Boolean.TYPE)) {
            return false;
        }
        
        // Check argument types
        Class<?>[] argtypes = m.getParameterTypes();
        if (!(argtypes.length == 1 && argtypes[0] == ActionContext.class)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Creates {@link ActionHandler} instance for the specified action method.
     * The {@link #isActionMethod(Method)} method should be used first to
     * determine if the specified method is an action method.
     * 
     * @param m Action method.
     * @return {@link ActionHandler} instance for the specified action method.
     * @see #isActionMethod(Method)
     */
    protected ActionHandler<?> createHandler(final Method m) {
        return new ActionHandler<DefaultParser<?>>() {
            final MethodInvoker invoker = MethodInvoker.newInvoker(m);
            public boolean handle(DefaultParser<?> parser, ActionContext context) throws Throwable {
                Object result = invoker.invoke(parser, context);
                return result instanceof Boolean ? (Boolean) result : true;
            }
        };
    }
    
}
