package io.peasoup.inv.loader;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import lombok.Getter;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;

import java.io.File;
import java.security.CodeSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class EncapsulatedGroovyClassLoader extends GroovyClassLoader {

    private static final Pattern WHITELISTED_REGEX = Pattern.compile(String.join("|", List.of(
            // Groovy stuff
            "groovy\\.grape\\..*", // example: groovy.grape.Grape
            "groovy\\.lang\\..*", // example: groovy.lang.Script
            "groovy\\.transform\\..*", // example: groovy.transform.Generated
            "groovy\\.util\\..*", // example: groovy.util.DelegatingScript
            "org\\.codehaus\\.groovy\\..*", // example: ...runtime.GeneratedClosure, ...runtime.callsite.CallSiteArray, ...reflection.ClassInfo

            // Junit
            "org\\.junit\\..*",
            "junit\\.framework\\..*",

            // INV stuff
            "io\\.peasoup\\.inv\\.testing\\.JunitScriptBase"
        )));

    private final Map<CodeSource, Config> knownConfigs = new ConcurrentHashMap<>();

    public EncapsulatedGroovyClassLoader(CompilerConfiguration config) {
        super(Thread.currentThread().getContextClassLoader(), config);
    }

    /**
     * Raised by the Groovy class parser
     * @param groovyCodeSource Groovy source code to parse
     * @param config Extended groovy classloader configuration
     * @return A class reference
     * @throws CompilationFailedException
     */
    public Class<?> parseClass(GroovyCodeSource groovyCodeSource, Config config) throws CompilationFailedException {
        this.knownConfigs.put(groovyCodeSource.getCodeSource(), config);

        return super.parseClass(groovyCodeSource);
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
        if (clazz.getClassLoader() instanceof EncapsulatedGroovyClassLoader)
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
        if (findClass.getClassLoader() instanceof EncapsulatedGroovyClassLoader)
            return findClass;

        // If nothing fits, raise exception
        throw new ClassNotFoundException(name);
    }

    public Config getConfig(CodeSource codeSource) {
        return this.knownConfigs.get(codeSource);
    }

    static class Config {

        private final String type;
        private final String packageName;
        private final boolean useScriptName;

        Config(String type, String packageName) {
            this(type, packageName, false);
        }

        Config(String type, String packageName, boolean useScriptName) {
            this.type = type;
            this.packageName = packageName;
            this.useScriptName = useScriptName;
        }

        public void updateClassnode(ModuleNode ast, ClassNode classNode) {
            // Default name equals to the source filename without the extension
            String newName = new File(ast.getDescription()).getName().split("\\.")[0];

            switch (type) {

                // Package is appended if available, class name is ignored and
                // replaced with a generic name + random suffix
                case "script":
                    if (!useScriptName)
                        newName = "Script" + random();

                    // Only if package is provided
                    if (StringUtils.isNotEmpty(packageName)) {
                        ast.setPackageName(packageName);
                        ast.addStarImport(packageName + ".");
                        classNode.setName(packageName + "." + newName);
                    } else {
                        classNode.setName(newName);
                    }

                    break;

                // Same as scripts, but package is required, so we assume its available
                case "test":
                    if (!useScriptName)
                        newName = "Test" + random();

                    // Only if package is provided
                    if (StringUtils.isNotEmpty(packageName)) {
                        ast.addStarImport(packageName + ".");
                        ast.setPackageName(packageName);
                        classNode.setName(packageName + "." + newName);
                    }

                    break;

                case "text":
                    // do nothing
                    break;
                default:
                    throw new IllegalStateException("type is not valid in this context");
            }
        }


        private String random() {
            return RandomStringUtils.random(9, true, true);
        }
    }

    // TODO Could this class replace completely Config (above)
    static class SourceUnit extends org.codehaus.groovy.control.SourceUnit {

        @Getter
        private final String packageName;

        public SourceUnit(File source, CompilerConfiguration configuration, GroovyClassLoader loader, ErrorCollector er, String packageName) {
            super(source, configuration, loader, er);

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

    }
}
