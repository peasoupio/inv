package io.peasoup.inv.security;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.TypeChecked;
import io.peasoup.inv.run.Logger;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;

public class CommonLoader {
    /**
     * ¸
     * Determine system-wide secure mode preference
     */
    public static void enableSecureMode() {
        systemSecureModeEnabled = true;
    }

    /**
     * Create a common loader using system-wide secure mode preference
     */
    public CommonLoader() {
        this(systemSecureModeEnabled);
    }

    /**
     * Create a common loader
     *
     * @param secureMode Determines if using secure mode or not
     */
    public CommonLoader(boolean secureMode) {

        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        secureASTCustomizer.setPackageAllowed(false);
        secureASTCustomizer.setIndirectImportCheckEnabled(false);

        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("extensions", "io.peasoup.inv.security.SecuredTypeChecked");
        ASTTransformationCustomizer astTransformationCustomizer = new ASTTransformationCustomizer(map, TypeChecked.class);

        final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);
        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer);

        this.secureMode = secureMode;
        this.generalClassLoader = new GroovyClassLoader(ClassLoader.getSystemClassLoader());
        this.securedClassLoader = new GroovyClassLoader(ClassLoader.getSystemClassLoader(), compilerConfiguration);
    }

    public Script parseClass(File file) throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        if (!secureMode) return (Script) generalClassLoader.parseClass(file).getDeclaredConstructor().newInstance();

        try {
            return (Script) securedClassLoader.parseClass(file).getDeclaredConstructor().newInstance();
        } catch (MultipleCompilationErrorsException ex) {
            if (hasFatalException(ex)) return null;
            else return (Script) generalClassLoader.parseClass(file).getDeclaredConstructor().newInstance();
        }

    }

    public Script parseClass(String text) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        if (!secureMode)
            return (Script)generalClassLoader.parseClass(text).getDeclaredConstructor().newInstance();

        try {
            return (Script) securedClassLoader.parseClass(text).getDeclaredConstructor().newInstance();
        } catch (MultipleCompilationErrorsException ex) {
            if (hasFatalException(ex)) return null;
            else return (Script) generalClassLoader.parseClass(text).getDeclaredConstructor().newInstance();
        }

    }

    public Script parseClass(String text, String fileName) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (!secureMode) return (Script) generalClassLoader.parseClass(text, fileName).getDeclaredConstructor().newInstance();

        try {
            return (Script) securedClassLoader.parseClass(text, fileName).getDeclaredConstructor().newInstance();
        } catch (MultipleCompilationErrorsException ex) {
            if (hasFatalException(ex)) return null;
            else return (Script) generalClassLoader.parseClass(text, fileName).getDeclaredConstructor().newInstance();
        }

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

    private static boolean systemSecureModeEnabled = false;
    private final boolean secureMode;
    private final GroovyClassLoader generalClassLoader;
    private final GroovyClassLoader securedClassLoader;

    public static class MethodCallNotAllowedException extends Exception {
        public MethodCallNotAllowedException(final ASTNode expr) {
            super("Method call is not allowed: " + expr.getText());
        }
    }
}