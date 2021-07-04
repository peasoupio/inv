package io.peasoup.inv.loader;

import groovy.lang.GroovyClassLoader;
import groovyjarjarasm.asm.ClassWriter;
import lombok.Getter;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class CustomClassLoader extends GroovyClassLoader {

    private static final Pattern WHITELISTED_REGEX = Pattern.compile(String.join("|", List.of(
            // Groovy stuff
            "groovy\\.grape\\..*", // example: groovy.grape.Grape
            "groovy\\.lang\\..*", // example: groovy.lang.Script
            "groovy\\.transform\\..*", // example: groovy.transform.Generated
            "groovy\\.util\\..*", // example: groovy.util.DelegatingScript
            "org\\.apache\\.groovy\\..*", // example: org.apache.groovy.ast.builder.AstBuilderTransformation
            "org\\.codehaus\\.groovy\\..*", // example: ...runtime.GeneratedClosure, ...runtime.callsite.CallSiteArray, ...reflection.ClassInfo

            // Junit
            "org\\.junit\\..*",
            "junit\\.framework\\..*",

            // INV stuff
            "io\\.peasoup\\.inv\\.testing\\.JunitScriptBase"
        )));

    @Getter
    private final CompilationUnit.ClassgenCallback classGenCallback = ((classVisitor, classNode) -> {
        String className = classNode.getName();
        byte[] bytecode = ((ClassWriter) classVisitor).toByteArray();

        Class<?> clazz = this.defineClass(className, bytecode);
        this.setClassCacheEntry(clazz);
    });

    public CustomClassLoader(CompilerConfiguration config) {
        super(Thread.currentThread().getContextClassLoader(), config);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = super.loadClass(name, resolve);

        // java stuff
        if (clazz.getClassLoader() == null)
            return clazz;

        // Custom classes
        if (getClassCacheEntry(name) != null)
            return clazz;

        // Preloaded third party classes
        if (clazz.getClassLoader() instanceof CustomClassLoader)
            return clazz;

        // If code source location is not a JAR file, it is safe
        // It is probably a Groovy class discovered by a folder set into the classpath
        if(!clazz.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"))
            return clazz;

        // Check if the class is whitelisted
        // IMPORTANT: This mechanism prevent "inv" classes and libraries cross-contamination
        if (WHITELISTED_REGEX.matcher(name).matches())
            return clazz;

        // Check if the class could be resolved by a grab
        // This case could occurs if this class is used by INV, thus not whitelisted
        Class<?> findClass = findClass(name);
        if (findClass.getClassLoader() instanceof CustomClassLoader)
            return findClass;

        // If nothing fits, raise exception
        throw new ClassNotFoundException(name);
    }

    static class SourceUnit extends org.codehaus.groovy.control.SourceUnit {

        @Getter
        private final String packageName;

        public SourceUnit(File source, CompilerConfiguration configuration, GroovyClassLoader loader, ErrorCollector er, String packageName) {
            super(source, configuration, loader, er);

            this.packageName = packageName;
        }

        public SourceUnit(String source, CompilerConfiguration configuration, GroovyClassLoader loader, ErrorCollector er, String packageName) {
            super("Script" + random(), source, configuration, loader, er);

            this.packageName = packageName;
        }

        public void updateAst(ClassNode classNode) {
            // Only if package is provided
            if (StringUtils.isEmpty(packageName))
                return;

            ast.addStarImport(packageName + ".");
            ast.setPackageName(packageName);

            String currentClassName = classNode.getName();
            if (currentClassName.contains("."))
                classNode.setName(packageName + "." + currentClassName.substring(currentClassName.lastIndexOf(".") + 1));
            else
                classNode.setName(packageName + "." + currentClassName);
        }

        private static String random() {
            return RandomStringUtils.random(9, true, true);
        }

    }
}
