package io.peasoup.inv.loader;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.security.CodeSource;
import java.util.*;
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

            // INV stuff
            "io\\.peasoup\\.inv\\.testing\\.JunitScriptBase"
        )));

    private final Map<CodeSource, Config> knownConfigs = new ConcurrentHashMap<>();

    public EncapsulatedGroovyClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config);
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

        Config(String type, String packageName) {
            this.type = type;
            this.packageName = packageName;
        }

        public void updateClassnode(ModuleNode ast, ClassNode classNode) {
            String newName;

            switch (type) {

                // Package is appended if available, class name is ignored and
                // replaced with a generic name + random suffix
                case "script":
                    newName = "Script" + random();
                    if (StringUtils.isNotEmpty(packageName)) {
                        ast.setPackageName(packageName);
                        ast.addStarImport(packageName + ".");
                        classNode.setName(packageName + "." + newName);
                    } else {
                        classNode.setName(newName);
                    }
                    break;

                // Takes for granted a "package.classname" signature is provided, then
                // replace the actual package with the one provided, but takes the actual classes name
                // Since a package could be provided, it's omitted and replaced with the one specified
                case "class":
                    ast.addStarImport(packageName + ".");
                    ast.setPackageName(packageName);

                    String currentClassName = classNode.getName();
                    if (currentClassName.contains("."))
                        classNode.setName(packageName + "." + currentClassName.substring(currentClassName.lastIndexOf(".") + 1));
                    else
                        classNode.setName(packageName + "." + currentClassName);
                    break;

                // Same as scripts, but package is required, so we assume its available
                case "test":
                    newName = "Test" + random();
                    ast.addStarImport(packageName + ".");
                    ast.setPackageName(packageName);
                    classNode.setName(packageName + "." + newName);
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
}
