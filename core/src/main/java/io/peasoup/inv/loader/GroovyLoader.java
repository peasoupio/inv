package io.peasoup.inv.loader;

import groovy.lang.Script;
import groovy.transform.TypeChecked;
import io.peasoup.inv.Home;
import io.peasoup.inv.Logger;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;

public class GroovyLoader {

    private static boolean systemSecureModeEnabled = false;

    /**
     * Enables system-wide secure mode.
     * Some API might be disabled, or key features, such as "packages" might be disabled also.
     */
    public static void enableSecureMode() {
        Logger.system("[GROOVYLOADER] secure: true");
        systemSecureModeEnabled = true;
    }

    /**
     * Disables system-wide secure mode.
     * Some API might be disabled, or key features, such as "packages" might be disabled also.
     */
    public static void disableSecureMode() {
        Logger.system("[GROOVYLOADER] secure: false");
        systemSecureModeEnabled = false;
    }

    /**
     * Create a new builder instance
     * @return The builder instance.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    private final boolean secureMode;

    private final CompilerConfiguration compilerConfiguration;

    private final CustomClassLoader generalClassLoader;
    private final CustomClassLoader securedClassLoader;

    private final CustomCompilationUnit classesCompilationUnit;

    /**
     * Create a common loader
     *
     * @param secureMode Determines if using secure mode or not
     * @param scriptBaseClass Determines the script base class. Must inherit groovy.lang.Script. If null or empty, default groovy base class is used.
     * @param importCustomizer A pre-defined import customizer. Can be null.
     */
    private GroovyLoader(boolean secureMode, String scriptBaseClass, ImportCustomizer importCustomizer) {
        this.secureMode = secureMode;

        this.compilerConfiguration = new CompilerConfiguration();
        this.compilerConfiguration.setTargetDirectory(Home.getClassesFolder());

        CompilerConfiguration securedCompilerConfiguration = new CompilerConfiguration();

        if (StringUtils.isNotEmpty(scriptBaseClass)) {
            this.compilerConfiguration.setScriptBaseClass(scriptBaseClass);
            securedCompilerConfiguration.setScriptBaseClass(scriptBaseClass);
        }

        if (importCustomizer != null) {
            this.compilerConfiguration.addCompilationCustomizers(importCustomizer);
            securedCompilerConfiguration.addCompilationCustomizers(importCustomizer);
        }

        // Apply SecureAST to all (de)compilers
        applySecureASTConfigs(securedCompilerConfiguration);

        // Apply SecureTypeChecker to secured (de)compiler
        applySecureTypeCheckerConfigs(securedCompilerConfiguration);

        this.securedClassLoader = new CustomClassLoader(securedCompilerConfiguration);

        // Create general classloader
        this.generalClassLoader = new CustomClassLoader(this.compilerConfiguration);

        // Create classes compilation unit
        this.classesCompilationUnit = new CustomCompilationUnit();
    }

    /**
     * Parse class from a Groovy script file, with a predefined package.
     * @param groovyFile Groovy file
     * @param packageName Defines package for groovy file classes (nullable)
     */
    public void addClassFile(File groovyFile, String packageName)  {
        classesCompilationUnit.addSourceWithPackage(
                        groovyFile,
                        packageName);
    }

    /**
     * Compile all class files.
     */
    public void compileClasses() {
        this.classesCompilationUnit.compile();
    }

    /**
     * Compile a Groovy text with preferred secure and classloading options
     *
     * @param text Groovy text
     * @return Compiled Object
     */
    public Class<?> parseClassText(String text)  {
        if (StringUtils.isEmpty(text))
            throw new IllegalArgumentException("text");

        CustomCompilationUnit compilationUnit = new CustomCompilationUnit();
        compilationUnit.addSourceWithPackage(
                text,
                null
        );

        return parseGroovyCodeSource(compilationUnit);
    }

    /**
     * Parse a test script class from a Groovy script file, with a predefined package.
     * @param groovyFile Groovy file
     * @param packageName Defines package for groovy file classes (nullable)
     * @return A new class object
     *
     * @throws IOException
     */
    public Class<?> parseTestScriptFile(File groovyFile, String packageName) throws IOException {
        CustomCompilationUnit compilationUnit = new CustomCompilationUnit();
        compilationUnit.addSourceWithPackage(
                groovyFile,
                packageName
        );

        return parseGroovyCodeSource(compilationUnit);
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param groovyFile Groovy file
     * @return A new Script instance
     *
     * @throws IOException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public Script parseScriptFile(File groovyFile) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return parseScriptFile(groovyFile, null);
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param groovyFile Groovy file
     * @param packageName Defines package for groovy file classes (nullable)
     * @return New script instance.
     *
     * @throws IOException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public Script parseScriptFile(File groovyFile, String packageName) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        CustomCompilationUnit compilationUnit = new CustomCompilationUnit();
        compilationUnit.addSourceWithPackage(
                groovyFile,
                packageName
        );

        return createScript(compilationUnit);
    }

    /**
     * Create a new instance of a Script Groovy code source
     * @param compilationUnit Compilation unit to use
     * @return A new Script instance
     */
    private Script createScript(CustomCompilationUnit compilationUnit) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Class<?> cls = parseGroovyCodeSource(compilationUnit);
        if (cls == null)
            return null;

        return (Script) cls.getDeclaredConstructor().newInstance();
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param compilationUnit Compilation unit to use
     * @return A new class object
     */
    private Class<?> parseGroovyCodeSource(CustomCompilationUnit compilationUnit) {

        SourceUnit sourceUnit = compilationUnit.getSourceUnit();
        if (sourceUnit == null)
            throw new IllegalStateException("Cannot retrieve source unit.");

        // If secure is enabled, use secure classloader
        if (secureMode) {
            try {
                Class<?> cls = securedClassLoader.parseClass(sourceUnit.getSource().getReader(), sourceUnit.getName());
                if (cls == null) {
                    Logger.warn("[COMMONLOADER] name: " + sourceUnit.getName() + ", succeeded: false");
                    return null;
                }

                // If this point is reached, source is valid and will be loaded into the general class loader.

            } catch (MultipleCompilationErrorsException ex) {
                if (hasFatalException(ex))
                    return null;
            } catch (IOException e) {
                Logger.error(e);
                return null;
            }
        }

        try {
            // Do the actual compilation
            compilationUnit.compile();

            // Expect the first class to be the "main" one, load it.
            return generalClassLoader.loadClass(compilationUnit.getClasses().get(0).getName());

        } catch (ClassNotFoundException | CompilationFailedException e) {
            Logger.error(e);
            return null;
        }
    }

    private boolean hasFatalException(MultipleCompilationErrorsException ex) {

        boolean returnValue = false;

        for(Message message : ex.getErrorCollector().getErrors()) {

            // Log Exception message
            // Exception Message does not loop for other exceptions and
            // returns to the caller immediately
            if (message instanceof ExceptionMessage) {
                ExceptionMessage exceptionMessage = (ExceptionMessage) message;
                Exception cause = exceptionMessage.getCause();

                if (cause instanceof MethodCallNotAllowedException) {
                    Logger.error(cause);
                    return true;
                }

                if (cause instanceof SecurityException) {
                    Logger.error(cause);
                    return true;
                }
            }

            // Synthax Message does immidiately and swallow the cause the type checking framework cannot recognize delegates inv, repo, ask, etc...
            if (message instanceof SyntaxErrorMessage) {
                SyntaxErrorMessage syntaxErrorMessage = (SyntaxErrorMessage) message;
                Exception cause = syntaxErrorMessage.getCause();

                // Do not track static type checking since it raises "false-positives"
                if (cause.getMessage().startsWith("[Static type checking] "))
                    continue;

                Logger.error(cause);
                returnValue = true;
            }
        }

        return returnValue;
    }

    /**
     * Apply custom AST transformer to trap type checking errors
     * @param compilerConfiguration
     */
    private void applySecureTypeCheckerConfigs(CompilerConfiguration compilerConfiguration) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("extensions", "io.peasoup.inv.loader.SecuredTypeChecked");
        ASTTransformationCustomizer astTransformationCustomizer = new ASTTransformationCustomizer(map, TypeChecked.class);
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);
    }

    private void applySecureASTConfigs(CompilerConfiguration compilerConfiguration) {
        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        secureASTCustomizer.setIndirectImportCheckEnabled(false);

        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer);
    }

    private class CustomCompilationUnit extends CompilationUnit {

        CustomCompilationUnit() {
            super(compilerConfiguration, null, generalClassLoader);

            this.setClassgenCallback(generalClassLoader.getClassGenCallback());
            this.addPhaseOperation((source, context, classNode) -> {
                if (source instanceof CustomClassLoader.SourceUnit) {
                    CustomClassLoader.SourceUnit su = (CustomClassLoader.SourceUnit) source;
                    su.updateAst(classNode);
                }
            }, Phases.CONVERSION);
        }

        /**
         * Gets first source unit in the queue list.
         * @return Sourceunit if found, otherwise null
         */
        SourceUnit getSourceUnit() {
            return this.queuedSources.peek();
        }

        /**
         * Create a new EncapsulatedGroovyClassLoader.SourceUnit instance from a groovy file
         * @param source The groovy file
         * @param packageName Sets a predefined package value
         */
        void addSourceWithPackage(File source, String packageName) {
            if (source == null)
                throw new IllegalArgumentException("source");

            this.addSource(new CustomClassLoader.SourceUnit(
                    source,
                    this.getConfiguration(),
                    this.getClassLoader(),
                    this.getErrorCollector(),
                    packageName
            ));
        }

        /**
         * Create a new EncapsulatedGroovyClassLoader.SourceUnit instance from source text
         * @param source The source
         * @param packageName Sets a predefined package value
         */
        void addSourceWithPackage(String source, String packageName) {
            if (StringUtils.isEmpty(source))
                throw new IllegalArgumentException("source");

            this.addSource(new CustomClassLoader.SourceUnit(
                    source,
                    this.getConfiguration(),
                    this.getClassLoader(),
                    this.getErrorCollector(),
                    packageName
            ));
        }
    }

    public static class Builder {

        private boolean secureMode = systemSecureModeEnabled;
        private String scriptBaseClass;
        private ImportCustomizer importCustomizer;

        private Builder() {
        }

        // boolean secureMode, String scriptBaseClass, ImportCustomizer importCustomizer

        /**
         * Sets the secure mode.
         * By default, secure mode uses the system-wide secure mode flag.
         * @return This builder.
         */
        public Builder secureMode(boolean secureMode) {
            this.secureMode = secureMode;

            return this;
        }

        /**
         * Sets the script base class for Script instantiation
         * @param scriptBaseClass The script base class name
         * @return This builder.
         */
        public Builder scriptBaseClass(String scriptBaseClass) {
            if (StringUtils.isEmpty(scriptBaseClass))
                throw new IllegalArgumentException(("scriptBaseClass"));

            this.scriptBaseClass = scriptBaseClass;

            return this;
        }

        /**
         * Sets a Import Customizer for classes to be instantiated.
         * @param importCustomizer The import customizer.
         * @return This builder.
         */
        public Builder importCustomizer(ImportCustomizer importCustomizer) {
            if (importCustomizer == null)
                throw new IllegalArgumentException("importCustomizer");

            this.importCustomizer = importCustomizer;

            return this;
        }

        /**
         * Build a new instance of GroovyLoader.
         * @return New GroovyLoader instance.
         */
        public GroovyLoader build() {
            return new GroovyLoader(
                    secureMode,
                    scriptBaseClass,
                    importCustomizer
            );
        }


    }

    public static class MethodCallNotAllowedException extends Exception {
        public MethodCallNotAllowedException(final ASTNode expr) {
            super("Method call is not allowed: " + expr.getText());
        }
    }
}
