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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.util.Map;
import java.util.HashMap;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.charset.Charset;

import org.foxlabs.peg4j.codegen.HtmlGenerator;
import org.foxlabs.peg4j.codegen.JavaGenerator;
import org.foxlabs.peg4j.codegen.TextGenerator;
import org.foxlabs.peg4j.debug.DebugTracer;
import org.foxlabs.peg4j.debug.TraceLevel;
import org.foxlabs.peg4j.grammar.Grammar;
import org.foxlabs.peg4j.grammar.GrammarCompiler;
import org.foxlabs.peg4j.grammar.GrammarParser;
import org.foxlabs.peg4j.resource.ResourceManager;

import org.foxlabs.util.PeriodCounter;

public final class CommandLine {
    
    /**
     * Default product name to be used if no manifest entry found.
     */
    static final String DEFAULT_NAME = "Peg4j";
    
    /**
     * Default product version to be used if no manifest entry found.
     */
    static final String DEFAULT_VERSION = "X.X.X";
    
    /**
     * Default product URL to be used if no manifest entry found.
     */
    static final String DEFAULT_URL = "http://foxlabs.org/p/peg4j/";
    
    /**
     * Returns product name.
     * 
     * @return Product name.
     */
    public static String getProductName() {
        String name = CommandLine.class.getPackage().getImplementationTitle();
        return name == null ? DEFAULT_NAME : name;
    }
    
    /**
     * Returns product version.
     * 
     * @return Product version.
     */
    public static String getProductVersion() {
        String version = CommandLine.class.getPackage().getImplementationVersion();
        return version == null ? DEFAULT_VERSION : version;
    }
    
    /**
     * Returns product URL.
     * 
     * @return Product URL.
     */
    public static String getProductURL() {
        String version = getProductVersion();
        return DEFAULT_URL + (DEFAULT_VERSION.equals(version) ? "" : version + "/");
    }
    
    // don't allow to create instances
    private CommandLine() {}
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        print(""); // just for visualization
        if (args.length < 2) {
            printUsage();
            terminateAbnormal(true);
        } else {
            try {
                Command command = createCommand(args[0]);
                setCommandArguments(command, args);
                command.execute(new File(args[args.length - 1]));
                terminateSuccessfully();
            } catch (Throwable e) {
                printError(e);
                terminateAbnormal(true);
            }
        }
    }
    
    /*
     * If this exception thrown then arguments not matches command line format.
     */
    static class CommandLineException extends RuntimeException {
        private static final long serialVersionUID = 5634864241563049519L;
        
        /*
         * Constructs a new command line exception with no detail message.
         */
        public CommandLineException() {
            super();
        }
        
        /*
         * Constructs a new command line exception with the specified
         * localized detail message.
         */
        public CommandLineException(String pattern, Object... args) {
            super(ResourceManager.getMessage(pattern, args));
        }
        
    }
    
    /*
     * Search for non abstract static Command subclass with name as the
     * specified command name and create instance of it class.
     * If command class doesn't exist then exception will be thrown.
     */
    static Command createCommand(String cmd) throws Throwable {
        // search for Command subclass
        for (Class<?> cc : CommandLine.class.getDeclaredClasses())
            if (cc.getSimpleName().equalsIgnoreCase(cmd)) {
                if (Command.class.isAssignableFrom(cc)) {
                    int mod = cc.getModifiers();
                    if (Modifier.isStatic(mod) && !Modifier.isAbstract(mod))
                        return (Command) cc.newInstance();
                }
            }
        
        // there is no command found
        throw new CommandLineException("cmd.unsupportedCommand", cmd);
    }
    
    /*
     * Set command options from command line. Each option represents setter
     * method in command class. Arguments of the setter method are command line
     * arguments followed by the command name.
     */
    static void setCommandArguments(Command command, String[] options) throws Throwable {
        Class<?> cc = command.getClass();
        String cmd = cc.getSimpleName().toLowerCase();
        
        for (int index = 1, lastIndex = options.length - 1; index < lastIndex;) {
            String option = options[index++];
            
            // option must start with "-"
            if (!(option.length() > 1 && option.startsWith("-")))
                throw new CommandLineException("cmd.illegalOption", option);
            
            // search for option setter in command class hierarchy
            Method sm = findOptionSetter(option.substring(1), cc);
            if (sm == null) // unsupported option
                throw new CommandLineException("cmd.unsupportedOption",
                        cmd, option);
            
            // decode option arguments
            Class<?>[] argtypes = sm.getParameterTypes();
            Object[] args = new Object[argtypes.length];
            for (int i = 0; i < argtypes.length; i++) {
                Decoder decoder = decoders.get(argtypes[i]);
                if (index == lastIndex) // insufficient option arguments
                    throw new CommandLineException("cmd.insufficientOptionArguments",
                            cmd, option, argtypes.length);
                try {
                    args[i] = decoder.decode(options[index++]);
                } catch (CommandLineException e) {
                    throw new CommandLineException("cmd.illegalOptionArgument",
                            cmd, option, i + 1, e.getMessage());
                }
            }
            
            // set option
            try {
                sm.invoke(command, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof CommandLineException)
                    throw e.getCause();
                throw e;
            }
        }
    }
    
    /*
     * Searches command option setter for specified option name.
     */
    static Method findOptionSetter(String name, Class<?> cc) {
        if (cc == Object.class)
            return null;
        // search in the command class first
        String setterName = "set" + name;
        for (Method sm : cc.getDeclaredMethods())
            if (sm.getName().equalsIgnoreCase(setterName))
                return sm;
        // search in parent class
        return findOptionSetter(name, cc.getSuperclass());
    }
    
    // ===== Decoders =========================================================
    
    /*
     * Decodes argument text representation to object.
     */
    interface Decoder {
        
        // Decodes specified text to value of some type.
        Object decode(String text) throws CommandLineException;
        
        // String
        Decoder STRING = new Decoder() {
            public Object decode(String text) {
                return text;
            }
        };
        
        // Integer
        Decoder INTEGER = new Decoder() {
            public Object decode(String text) {
                try {
                    return Integer.valueOf(text);
                } catch (NumberFormatException e) {
                    throw new CommandLineException("cmd.illegalIntValue", text);
                }
            }
        };
        
        // Class
        Decoder CLASS = new Decoder() {
            public Object decode(String text) {
                try {
                    return Class.forName(text);
                } catch (ClassNotFoundException e) {
                    throw new CommandLineException("cmd.classNotFound", text);
                }
            }
        };
        
        // File
        Decoder FILE = new Decoder() {
            public Object decode(String text) {
                return new File(text);
            }
        };
        
        // Enum
        static final class Enumeration implements Decoder {
            
            final Enum<?>[] constants;
            
            Enumeration(Class<? extends Enum<?>> type) {
                constants = type.getEnumConstants();
            }
            
            public Object decode(String text) {
                for (Enum<?> constant : constants)
                    if (constant.name().equalsIgnoreCase(text))
                        return constant;
                throw new CommandLineException("cmd.illegalEnumValue", text);
            }
            
        }
        
    }
    
    // All required argument decoders.
    static final Map<Class<?>, Decoder> decoders;
    
    static {
        decoders = new HashMap<Class<?>, Decoder>();
        decoders.put(String.class, Decoder.STRING);
        decoders.put(Integer.class, Decoder.INTEGER);
        decoders.put(Integer.TYPE, Decoder.INTEGER);
        decoders.put(Class.class, Decoder.CLASS);
        decoders.put(File.class, Decoder.FILE);
        decoders.put(TraceLevel.class, new Decoder.Enumeration(TraceLevel.class));
    }
    
    // ===== Commands =========================================================
    
    /*
     * Base command abstract class that maintains common options and compiles
     * source grammar file.
     */
    static abstract class Command {
        
        // grammar source file encoding
        protected String encoding;
        // grammar compilation flags
        private int flags;
        
        // compiled grammar object
        protected Grammar grammar;
        
        // -encoding <charset>
        public void setEncoding(String value) {
            if (!Charset.isSupported(value))
                throw new CommandLineException("cmd.unsupportedEncoding", value);
            encoding = value;
        }
        
        // -sw
        public void setSW() {
            setSuppressWarnings();
        }
        
        // -suppresswarnings
        public void setSuppressWarnings() {
            flags |= GrammarCompiler.SUPPRESS_WARNINGS;
        }
        
        // -sh
        public void setSH() {
            setSuppressHints();
        }
        
        // -suppresshints
        public void setSuppressHints() {
            flags |= GrammarCompiler.SUPPRESS_HINTS;
        }
        
        // -ms
        public void setMS() {
            setMakeSuggestions();
        }
        
        // -makesuggestions
        public void setMakeSuggestions() {
            flags |= GrammarCompiler.MAKE_SUGGESTIONS;
        }
        
        public void execute(File source) throws Throwable {
            if (encoding == null)
                encoding = Charset.defaultCharset().name();
            
            GrammarParser parser = new GrammarParser();
            grammar = parser.parse(source, encoding);
            GrammarCompiler.compile(grammar, flags);
            
            if (grammar.hasProblems()) {
                print(grammar.getProblems().toString());
                if (grammar.hasErrors())
                    terminateAbnormal(false);
            }
        }
        
    }
    
    /*
     * This command compiles grammar source file.
     */
    static final class Compile extends Command {
        
        public void execute(File source) throws Throwable {
            super.execute(source);
        }
        
    }
    
    /*
     * This command traces specified source file using compiled grammar and
     * writes trace log file.
     */
    static final class Trace extends Command {
        
        // source file to trace (required)
        private File source;
        // trace log file
        private File log;
        // trace level
        private TraceLevel level = TraceLevel.MEDIUM;
        // max logging depth
        private int maxDepth = 20;
        // rule source logging text max length
        private int maxTextLength = 1024;
        
        // -source <file>
        public void setSource(File value) {
            source = value;
        }
        
        // -log <file>
        public void setLog(File value) {
            log = value;
        }
        
        // -level <LOW|MEDIUM|HIGH>
        public void setLevel(TraceLevel value) {
            level = value;
        }
        
        // -maxdepth <int>
        public void setMaxDepth(int value) {
            maxDepth = value;
        }
        
        // -maxtextlength <int>
        public void setMaxTextLength(int value) {
            maxTextLength = value;
        }
        
        public void execute(File source) throws Throwable {
            if (this.source == null)
                throw new CommandLineException("cmd.traceSourceRequired");
            
            super.execute(source);
            
            if (level == null)
                level = TraceLevel.MEDIUM;
            if (log == null)
                log = changeExt(source, "log");
            
            Parser<Object> parser = new Parser<Object>() {
                protected Grammar getGrammar() {
                    return grammar;
                }
                protected Transaction startTransaction() {
                    return new Transaction.Default();
                }
                protected Object buildResult() {
                    return null;
                }
            };
            
            DebugTracer tracer = new DebugTracer(log);
            tracer.setTraceLevel(level);
            tracer.setMaxDepthLevel(maxDepth);
            tracer.setMaxTextSize(maxTextLength);
            parser.setTracer(tracer);
            
            parser.parse(this.source);
        }
        
    }
    
    /*
     * This command generates compiled grammar into java Parser subclass that
     * contains grammar building statements.
     */
    static final class Java extends Command {
        
        // parser class simple name
        private String name;
        // parser class package name
        private String namespace;
        // result type name
        private Class<?> result;
        // java class output directory
        private File outdir;
        // generation flags
        private int flags;
        
        // set default flags
        Java() {
            flags |= JavaGenerator.GENERATE_COMMENTS;
        }
        
        // -name <class name>
        public void setName(String value) {
            name = value;
        }
        
        // -package <class package>
        public void setPackage(String value) {
            namespace = value;
        }
        
        // -result <class name>
        public void setResult(Class<?> value) {
            result = value;
        }
        
        // -outdir <dir>
        public void setOutdir(File value) {
            outdir = value;
        }
        
        // -debuginfo
        public void setDebugInfo() {
            flags |= JavaGenerator.INCLUDE_DEBUGINFO;
        }
        
        // -nocomments
        public void setNoComments() {
            flags &= ~JavaGenerator.GENERATE_COMMENTS;
        }
        
        public void execute(File source) throws Throwable {
            super.execute(source);
            
            if (outdir == null)
                outdir = source.getParentFile();
            
            String filename = name == null ? "MyParser" : name;
            FileWriter out = new FileWriter(new File(outdir, filename + ".java"));
            try {
                JavaGenerator jg = new JavaGenerator(name, namespace, result, flags);
                jg.generate(grammar, out);
            } finally {
                out.close();
            }
        }
        
    }
    
    /*
     * This command generates compiled grammar into text source representation.
     */
    static final class Text extends Command {
        
        // text source output file
        private File target;
        // generation flags
        private int flags;
        
        // set default flags
        Text() {
            flags |= TextGenerator.GENERATE_COMMENTS;
        }
        
        // -target <file>
        public void setTarget(File value) {
            target = value;
        }
        
        // -nocomments
        public void setNoComments() {
            flags &= ~TextGenerator.GENERATE_COMMENTS;
        }
        
        public void execute(File source) throws Throwable {
            super.execute(source);
            
            if (target == null)
                target = changeExt(source, "peg");
            
            FileWriter out = new FileWriter(target);
            try {
                TextGenerator tg = new TextGenerator(flags);
                tg.generate(grammar, out);
            } finally {
                out.close();
            }
        }
        
    }
    
    /*
     * This command generates compiled grammar into HTML document
     * representation.
     */
    static final class Html extends Command {
        
        // HTML document output file
        private File document;
        // CSS theme file
        private File theme;
        // document title
        private String title;
        // generation flags
        private int flags;
        
        // set default flags
        Html() {
            flags |= HtmlGenerator.SHOW_LINENUMBERS;
            flags |= HtmlGenerator.COMMENT_PROBLEMS;
        }
        
        // -document <file>
        public void setDocument(File value) {
            document = value;
        }
        
        // -theme <file>
        public void setTheme(File value) {
            theme = value;
        }
        
        // -title <name>
        public void setTitle(String value) {
            title = value;
        }
        
        // -nolines
        public void setNoLines() {
            flags &= ~HtmlGenerator.SHOW_LINENUMBERS;
        }
        
        // -noproblems
        public void setNoProblems() {
            flags &= ~HtmlGenerator.COMMENT_PROBLEMS;
        }
        
        // -injections
        public void setInjections() {
            flags |= HtmlGenerator.PRINT_INJECTIONS;
        }
        
        // -hloff
        public void setHlOff() {
            flags |= HtmlGenerator.HIGHLIGHTING_OFF;
        }
        
        public void execute(File source) throws Throwable {
            super.execute(source);
            
            if (document == null)
                document = changeExt(source, "html");
            
            String styles = null;
            if (theme != null) {
                FileReader in = new FileReader(theme);
                try {
                    char[] chunk = new char[8192];
                    StringBuilder buf = new StringBuilder();
                    for (int len = in.read(chunk); len > 0; len = in.read(chunk)) {
                        buf.append(chunk, 0, len);
                    }
                    styles = buf.toString();
                } finally {
                    in.close();
                }
            }
            
            FileWriter out = new FileWriter(document);
            try {
                HtmlGenerator hg = new HtmlGenerator(title, styles, flags);
                hg.generate(grammar, out);
            } finally {
                out.close();
            }
        }
        
    }
    
    // ===== Utils ============================================================
    
    /*
     * Returns new file that is source file with replaced extension.
     */
    static File changeExt(File source, String ext) {
        String filename = source.getPath();
        int index = filename.lastIndexOf('.');
        if (index >= 0)
            filename = filename.substring(0, index + 1);
        return new File(filename + ext);
    }
    
    /*
     * Prints message to the system ERR.
     */
    static void print(String message) {
        System.err.println(message);
    }
    
    /*
     * Prints localized message.
     */
    static void printPattern(String pattern, Object... args) {
        print(ResourceManager.getMessage(pattern, args));
    }
    
    /*
     * Prints error info.
     */
    static void printError(Throwable e) {
        if (e instanceof CommandLineException) {
            printUsage();
            if (e.getMessage() != null)
                print(e.getMessage());
        } else if (e instanceof Peg4jException) {
            printPattern("cmd.specificationError", e.getMessage());
        } else if (e instanceof SyntaxException) {
            printPattern("cmd.syntaxError", e.getMessage());
        } else if (e instanceof IOException) {
            printPattern("cmd.ioError", e.getMessage());
        } else {
            e.printStackTrace();
        }
    }
    
    /*
     * Prints usage info.
     */
    static void printUsage() {
        print(ResourceManager.getCommandLineUsage());
    }
    
    /*
     * Prints took time.
     */
    static void printTook() {
        print(PeriodCounter.encodeDuration(System.currentTimeMillis() - STARTUP));
    }
    
    /*
     * Terminate execution successfully.
     * Can be called inside main method only.
     */
    static void terminateSuccessfully() {
        printPattern("cmd.success");
        printTook();
        System.exit(0);
    }
    
    /*
     * Terminate execution abnormally.
     */
    static void terminateAbnormal(boolean silent) {
        if (!silent) {
            printPattern("cmd.failure");
            printTook();
        }
        System.exit(1);
    }
    
    static final long STARTUP = System.currentTimeMillis();
    
}
