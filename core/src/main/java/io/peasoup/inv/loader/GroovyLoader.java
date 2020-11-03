package io.peasoup.inv.loader;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;
import groovy.transform.TypeChecked;
import io.peasoup.inv.run.Logger;
import io.peasoup.inv.run.RunsRoller;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private final boolean systemClassloader;
    private final GroovyClassLoader generalClassLoader;
    private final GroovyClassLoader securedClassLoader;


    /**
     * Create a common loader using system-wide secure mode preference
     */
    public GroovyLoader() {
        this(systemSecureModeEnabled, systemClassloaderEnabled,  null, null);
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
        this.systemClassloader = systemClassloader;

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

        ClassLoader loaderToUse = Thread.currentThread().getContextClassLoader();
        if (systemClassloader)
            loaderToUse = ClassLoader.getSystemClassLoader();


        // Apply SecureAST to all (de)compilers
        applySecureASTConfigs(securedCompilerConfiguration);

        // Apply SecureTypeChecker to secured (de)compiler
        applySecureTypeCheckerConfigs(securedCompilerConfiguration);

        this.generalClassLoader = new GroovyClassLoader(loaderToUse, compilerConfiguration);
        this.securedClassLoader = new GroovyClassLoader(loaderToUse, securedCompilerConfiguration);
    }

    /**
     * Compile a Groovy text with preferred secure and classloading options
     *
     * @param text Groovy text
     * @return Compiled Object
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public Class<?> parseClassText(String text) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {
        return parseGroovyCodeSource(new GroovyCodeSource(
                text,
                "Script" + checksum(),
                "groovy/script"));
    }

    /**
     * Parse class from a Groovy script file, with a predefined package.
     * @param groovyFile Groovy file
     * @param newPackage Defines package for groovy file classes (nullable)
     * @return
     * @throws IOException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public Class<?> parseClassFile(File groovyFile, String newPackage) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (StringUtils.isEmpty(newPackage))
            throw new IllegalArgumentException("newPackage");

        String className = newPackage + "." + groovyFile.getName().split("\\.")[0];

        return parseGroovyCodeSource(new GroovyCodeSource(
                new FileReader(groovyFile),
                className,
                groovyFile.getAbsolutePath()));
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param groovyFile Groovy file
     * @return
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
        String className;
        if (StringUtils.isNotEmpty(newPackage))
            className = newPackage + ".Script" + checksum();
        else
            className = normalizeGroovyFilename(groovyFile);

        return createScript(new GroovyCodeSource(
                new FileReader(groovyFile),
                className,
                groovyFile.getAbsolutePath()));
    }

    /**
     * Create a new instance of a Script Groovy code source
     * @param groovyCodeSource Groovy code source
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IOException
     */
    private Script createScript(GroovyCodeSource groovyCodeSource) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {
        // Otherwise, use general classloader
        Class<?> cls = parseGroovyCodeSource(groovyCodeSource);

        return (Script)cls.getDeclaredConstructor().newInstance();
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param groovyCodeSource Groovy code source
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IOException
     */
    private Class<?> parseGroovyCodeSource(GroovyCodeSource groovyCodeSource) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {

        // If secure is enabled, use secure classloader
        if (secureMode) {
            try {
                Class<?> cls = securedClassLoader.parseClass(groovyCodeSource);
                if (cls == null) {
                    Logger.warn("[COMMONLOADER] name: " + groovyCodeSource.getName() + ", succeeded: false");
                    return null;
                }

                return cls;
            } catch (MultipleCompilationErrorsException ex) {
                if (hasFatalException(ex))
                    return null;
            }
        }

        // Otherwise, use general classloader
        Class<?> cls = generalClassLoader.parseClass(groovyCodeSource);
        if (cls == null) {
            Logger.warn("[COMMONLOADER] name: " + groovyCodeSource.getName() + ", succeeded: false");
            return null;
        }

        return cls;
    }

    private boolean hasFatalException(MultipleCompilationErrorsException ex) {

        for(Message message : ex.getErrorCollector().getErrors()) {
            if (!(message instanceof ExceptionMessage))
                continue;

            ExceptionMessage exceptionMessage = (ExceptionMessage)message;
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

        return false;
    }

    private File cache(File scriptFile, final String classname) throws IOException {
        if (scriptFile == null) {
            throw new IllegalArgumentException("Script file is required");
        }
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Script file must exists");
        }
        if (StringUtils.isEmpty(classname)) {
            throw new IllegalArgumentException("Classname is required");
        }

        File cache = new File(RunsRoller.getLatest().folder(), "scripts/");

        // Make sure cache is available with minimal accesses
        if (!cache.exists()) {
            Logger.system("[CACHE] folder: " + cache.getAbsolutePath() + ", created: " + cache.mkdirs());

            // https://stackoverflow.com/questions/5302269/java-file-setwritable-and-stopped-working-correctly-after-jdk-6u18
            if (!cache.setExecutable(true)) {
                throw new IllegalArgumentException("Could not set executable");
            }

            boolean writabledSet = cache.setWritable(true, false);
            Logger.system("[SECURITY] writable: " + writabledSet);

            if (!cache.setReadable(true, false)) {
                throw new IllegalArgumentException("Could not set readable");
            }
        }

        final File cacheFile = new File(cache, classname + ".groovy");
        Logger.system("[CACHE] folder: " + classname + ", created: " + cacheFile.getParentFile().mkdirs());

        // Make sure we got latest
        if (cacheFile.exists())
            Files.delete(Paths.get(cacheFile.getAbsolutePath()));

        // Create a symlink to have dynamic updates adn save space
        //Files.createSymbolicLink(Paths.get(filename.absolutePath), Paths.get(scriptFile.absolutePath))
        Files.copy(Paths.get(scriptFile.getAbsolutePath()), Paths.get(cacheFile.getAbsolutePath()));

        Logger.system("[CACHE] file: " + cacheFile.getName());

        return cacheFile;
    }

    private String normalizeGroovyFilename(File script) {
        if (script.getParent() == null) return script.getName().split("\\.")[0];

        if (script.getName().equalsIgnoreCase("inv")) return script.getParentFile().getName();

        if (script.getName().equalsIgnoreCase("inv.groovy")) return script.getParentFile().getName();

        return script.getName().split("\\.")[0];
    }

    private String checksum() {
        return RandomStringUtils.random(9, true, true);
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
        secureASTCustomizer.setPackageAllowed(false);
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
