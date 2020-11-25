package io.peasoup.inv.loader;

import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;
import groovy.transform.TypeChecked;
import io.peasoup.inv.Logger;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
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

    private static boolean systemSecureModeEnabled = false;

    /**
     * Enables SystemClassloader.
     * It allows GrabConfig(systemClassLoader=true).
     */
    public static void enableSystemClassloader() {
        Logger.system("[GROOVYLOADER] system: true");
        systemClassloaderEnabled = true; }

    /**
     * Disables SystemClassloader.
     * It allows GrabConfig(systemClassLoader=true).
     */
    public static void disableSystemClassloader() {
        Logger.system("[GROOVYLOADER] system: false");
        systemClassloaderEnabled = false; }
    private static boolean systemClassloaderEnabled = false;


    private final boolean secureMode;
    private final EncapsulatedGroovyClassLoader generalClassLoader;
    private final EncapsulatedGroovyClassLoader securedClassLoader;


    /**
     * Create a common loader using system-wide secure mode preference
     */
    public GroovyLoader() {
        this(systemSecureModeEnabled, systemClassloaderEnabled,  null, null);
    }

    /**
     * Create a common loader using system-wide secure mode preference
     *
     * @param scriptBaseClass Determines the script base class. Must inherit groovy.lang.Script. If null or empty, default groovy base class is used.
     */
    public GroovyLoader(String scriptBaseClass) {
        this(systemSecureModeEnabled, systemClassloaderEnabled,  scriptBaseClass, null);
    }

    /**
     * Create a common loader
     *
     * @param secureMode Determines if using secure mode or not
     * @param scriptBaseClass Determines the script base class. Must inherit groovy.lang.Script. If null or empty, default groovy base class is used.
     * @param importCustomizer A pre-defined import customizer. Can be null.
     */
    public GroovyLoader(boolean secureMode, String scriptBaseClass, ImportCustomizer importCustomizer) {
        this(secureMode, systemClassloaderEnabled,  scriptBaseClass, importCustomizer);
    }

    /**
     * Create a common loader
     *
     * @param secureMode Determines if using secure mode or not
     * @param systemClassloader Determines if using system classloader
     * @param scriptBaseClass Determines the script base class. Must inherit groovy.lang.Script. If null or empty, default groovy base class is used.
     * @param importCustomizer A pre-defined import customizer. Can be null.
     */
    public GroovyLoader(boolean secureMode, boolean systemClassloader, String scriptBaseClass, ImportCustomizer importCustomizer) {
        this.secureMode = secureMode;

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        CompilerConfiguration securedCompilerConfiguration = new CompilerConfiguration();

        if (StringUtils.isNotEmpty(scriptBaseClass)) {
            compilerConfiguration.setScriptBaseClass(scriptBaseClass);
            securedCompilerConfiguration.setScriptBaseClass(scriptBaseClass); // TODO Is it safe ?
        }

        if (importCustomizer != null) {
            compilerConfiguration.addCompilationCustomizers(importCustomizer);
            securedCompilerConfiguration.addCompilationCustomizers(importCustomizer);
        }

        compilerConfiguration.addCompilationCustomizers(new PackageTransformationCustomizer());

        /*
        ClassLoader loaderToUse = Thread.currentThread().getContextClassLoader();
        if (systemClassloader)
            loaderToUse = ClassLoader.getSystemClassLoader();

        */
        ClassLoader loaderToUse = Thread.currentThread().getContextClassLoader();

        // Apply SecureAST to all (de)compilers
        applySecureASTConfigs(securedCompilerConfiguration);

        // Apply SecureTypeChecker to secured (de)compiler
        applySecureTypeCheckerConfigs(securedCompilerConfiguration);

        this.generalClassLoader = new EncapsulatedGroovyClassLoader(loaderToUse, compilerConfiguration);
        this.securedClassLoader = new EncapsulatedGroovyClassLoader(loaderToUse, securedCompilerConfiguration);
    }

    /**
     * Compile a Groovy text with preferred secure and classloading options
     *
     * @param text Groovy text
     * @return Compiled Object
     */
    public Class<?> parseClassText(String text)  {
        return parseGroovyCodeSource(
            new GroovyCodeSource(
                text,
                "script:",
                "groovy/script"),
            new EncapsulatedGroovyClassLoader.Config("text", null));
    }

    /**
     * Parse class from a Groovy script file, with a predefined package.
     * @param groovyFile Groovy file
     * @param packageName Defines package for groovy file classes (nullable)
     * @return A new class object
     * @throws IOException
     */
    public Class<?> parseClassFile(File groovyFile, String packageName) throws IOException {
        if (StringUtils.isEmpty(packageName))
            throw new IllegalArgumentException("packageName");

        return parseGroovyCodeSource(
                new GroovyCodeSource(groovyFile),
                new EncapsulatedGroovyClassLoader.Config("class", packageName));
    }

    /**
     * Parse a test script class from a Groovy script file, with a predefined package.
     * @param groovyFile Groovy file
     * @param packageName Defines package for groovy file classes (nullable)
     * @return A new class object
     * @throws IOException
     */
    public Class<?> parseTestScriptFile(File groovyFile, String packageName) throws IOException {
        if (StringUtils.isEmpty(packageName))
            throw new IllegalArgumentException("packageName");

        return parseGroovyCodeSource(
                new GroovyCodeSource(groovyFile),
                new EncapsulatedGroovyClassLoader.Config("test", packageName));
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param groovyFile Groovy file
     * @return A new Script instance
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
     * @param newPackage Defines package for groovy file classes (nullable)
     * @return
     * @throws IOException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public Script parseScriptFile(File groovyFile, String newPackage) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return createScript(
                new GroovyCodeSource(groovyFile),
                new EncapsulatedGroovyClassLoader.Config("script", newPackage));
    }

    /**
     * Create a new instance of a Script Groovy code source
     * @param groovyCodeSource Groovy code source
     * @param config Extended Groovy class loader config
     * @return A new Script instance
     */
    private Script createScript(GroovyCodeSource groovyCodeSource, EncapsulatedGroovyClassLoader.Config config) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        Class<?> cls = parseGroovyCodeSource(groovyCodeSource, config);
        if (cls == null)
            return null;

        return (Script) cls.getDeclaredConstructor().newInstance();
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param groovyCodeSource Groovy code source
     * @param config Extended Groovy class loader config
     * @return A new class object
     */
    private Class<?> parseGroovyCodeSource(GroovyCodeSource groovyCodeSource, EncapsulatedGroovyClassLoader.Config config) {

        // If secure is enabled, use secure classloader
        if (secureMode) {
            try {
                Class<?> cls = securedClassLoader.parseClass(groovyCodeSource, config);
                if (cls == null) {
                    Logger.warn("[COMMONLOADER] name: " + groovyCodeSource.getName() + ", succeeded: false");
                    return null;
                }

                // If this point is reached, source is valid and will be loaded into the general class loader.

            } catch (MultipleCompilationErrorsException ex) {
                if (hasFatalException(ex))
                    return null;
            }
        }

        // Otherwise, use general classloader
        Class<?> cls = generalClassLoader.parseClass(groovyCodeSource, config);
        if (cls == null) {
            Logger.warn("[COMMONLOADER] name: " + groovyCodeSource.getName() + ", succeeded: false");
            return null;
        }

        return cls;
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

    private CompilerConfiguration applySecureTypeCheckerConfigs(CompilerConfiguration compilerConfiguration) {

        // Apply custom AST transformer to trap type checking errors
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("extensions", "io.peasoup.inv.loader.SecuredTypeChecked");
        ASTTransformationCustomizer astTransformationCustomizer = new ASTTransformationCustomizer(map, TypeChecked.class);
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);

        return compilerConfiguration;
    }

    private CompilerConfiguration applySecureASTConfigs(CompilerConfiguration compilerConfiguration) {
        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        secureASTCustomizer.setIndirectImportCheckEnabled(false);

        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer);

        return compilerConfiguration;
    }

    public static class MethodCallNotAllowedException extends Exception {
        public MethodCallNotAllowedException(final ASTNode expr) {
            super("Method call is not allowed: " + expr.getText());
        }
    }
}
