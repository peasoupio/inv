package io.peasoup.inv.loader;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.TypeChecked;
import io.peasoup.inv.run.Logger;
import io.peasoup.inv.run.RunsRoller;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;

public class GroovyLoader {


    /**
     * Enables system-wide secure mode.
     * Some API might be disabled, or key features, such as "packages" might be disabled also.
     */
    public static void enableSecureMode() {
        systemSecureModeEnabled = true;
    }
    private static boolean systemSecureModeEnabled = false;

    /**
     * Enables SystemClassloader.
     * It allows GrabConfig(systemClassLoader=true).
     */
    public static void enableSystemClassloader() { systemClassloaderEnabled = true; }
    private static boolean systemClassloaderEnabled = false;

    private final boolean secureMode;
    private final GroovyClassLoader generalClassLoader;
    private final GroovyClassLoader securedClassLoader;


    /**
     * Create a common loader using system-wide secure mode preference
     */
    public GroovyLoader() {
        this(systemSecureModeEnabled, null, null);
    }

    /**
     * Create a common loader
     *
     * @param secureMode Determines if using secure mode or not
     * @param scriptBaseClass Determines the script base class. Must inherit groovy.lang.Script. If null or empty, default groovy base class is used.
     * @param importCustomizer A pre-defined import customizer. Can be null.
     */
    public GroovyLoader(boolean secureMode, String scriptBaseClass, ImportCustomizer importCustomizer) {
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

        ClassLoader loaderToUse = Thread.currentThread().getContextClassLoader();
        if (systemClassloaderEnabled) {
            loaderToUse = ClassLoader.getSystemClassLoader();
            Logger.system("[CLASSLOADER] system: true");
        } else {
            Logger.system("[CLASSLOADER] system: false");
        }

        this.generalClassLoader = new GroovyClassLoader(loaderToUse, compilerConfiguration);
        this.securedClassLoader = new GroovyClassLoader(loaderToUse, applySecureTransformers(securedCompilerConfiguration));
    }

    /**
     * Compile a Groovy text with preferred secure and classloading options.
     * This method does not cache the Groovy script file.
     * For caching, use "parseClass".
     *
     * @param file Groovy file
     * @return Compiled Object
     *
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public Script parseText(File file) throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return parseText(ResourceGroovyMethods.getText(file));
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
    public Script parseText(String text) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        // If secure is enabled, use secure classloader
        if (secureMode) {
            try {
                Class<?> cls = securedClassLoader.parseClass(text);
                if (cls == null) throw new IllegalStateException("text could not be parsed as a Class object");

                return (Script)cls.getDeclaredConstructor().newInstance();
            } catch (MultipleCompilationErrorsException ex) {
                if (hasFatalException(ex))
                    return null;
            }
        }

        // Otherwise, use general classloader
        Class<?> cls = generalClassLoader.parseClass(text);
        if (cls == null) return null;

        return (Script)cls.getDeclaredConstructor().newInstance();
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
    public Script parseClass(File groovyFile) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return parseClass(ResourceGroovyMethods.getText(groovyFile), groovyFile);
    }

    /**
     * Parse and raise new instance of Groovy (script) file
     * @param text Groovy file text
     * @param groovyFile Groovy file location
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IOException
     */
    public Script parseClass(String text, File groovyFile) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {
        // Get preferred classname
        String preferredClassname = (normalizeClassName(groovyFile) + "_" + checksum(groovyFile)).toLowerCase();

        // Cache the file
        cache(groovyFile, preferredClassname);

        // If secure is enabled, use secure classloader
        if (secureMode) {
            try {
                Class<?> cls = securedClassLoader.parseClass(text, preferredClassname);
                if (cls == null) {
                    Logger.warn("[COMMONLOADER] file: " + preferredClassname + ", succeeded: false");
                    return null;
                }

                return (Script)cls.getDeclaredConstructor().newInstance();
            } catch (MultipleCompilationErrorsException ex) {
                if (hasFatalException(ex))
                    return null;
            }
        }

        // Otherwise, use general classloader
        Class<?> cls = generalClassLoader.parseClass(text, preferredClassname);
        if (cls == null) {
            Logger.warn("[COMMONLOADER] file: " + preferredClassname + ", succeeded: false");
            return null;
        }

        return (Script)cls.getDeclaredConstructor().newInstance();
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

    private String cache(File scriptFile, final String classname) throws IOException {
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

        final File filename = new File(cache, classname + ".groovy");
        Logger.system("[CACHE] folder: " + classname + ", created: " + filename.getParentFile().mkdirs());

        // Make sure we got latest
        if (filename.exists())
            Files.delete(Paths.get(filename.getAbsolutePath()));

        // Create a symlink to have dynamic updates adn save space
        //Files.createSymbolicLink(Paths.get(filename.absolutePath), Paths.get(scriptFile.absolutePath))
        Files.copy(Paths.get(scriptFile.getAbsolutePath()), Paths.get(filename.getAbsolutePath()));

        Logger.system("[CACHE] file: " + filename.getName());

        return filename.getAbsolutePath();
    }

    private String normalizeClassName(File script) {
        if (script.getParent() == null) return script.getName().split("\\.")[0];

        if (script.getName().equalsIgnoreCase("inv")) return script.getParentFile().getName();

        if (script.getName().equalsIgnoreCase("inv.groovy")) return script.getParentFile().getName();

        return script.getName().split("\\.")[0];
    }

    private String checksum(File path) throws IOException {

        String checksumValue = path.getName();

        try {
            try (
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos)
            ) {
                oos.writeObject(path.getAbsolutePath());
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(baos.toByteArray());
                checksumValue = DatatypeConverter.printHexBinary(thedigest);
            }
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e);
        }

        return checksumValue;
    }

    private CompilerConfiguration applySecureTransformers(CompilerConfiguration compilerConfiguration) {

        // Apply custom AST transformer to trap type checking errors
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("extensions", "io.peasoup.inv.loader.SecuredTypeChecked");
        ASTTransformationCustomizer astTransformationCustomizer = new ASTTransformationCustomizer(map, TypeChecked.class);
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);

        // Apply generalized secured configurations
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
