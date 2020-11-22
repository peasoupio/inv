package io.peasoup.inv.loader;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import spark.utils.StringUtils;

import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExtGroovyClassLoader extends GroovyClassLoader {

    private final Map<CodeSource, ExtConfig> knownConfigs = new ConcurrentHashMap<>();

    public ExtGroovyClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config);
    }

    public Class parseClass(GroovyCodeSource groovyCodeSource, ExtConfig extConfig) throws CompilationFailedException {
        this.knownConfigs.put(groovyCodeSource.getCodeSource(), extConfig);

        return super.parseClass(groovyCodeSource);
    }

    public ExtConfig getExtConfig(CodeSource codeSource) {
        return this.knownConfigs.get(codeSource);
    }


    static class ExtConfig {

        private final String type;
        private final String packageName;

        ExtConfig(String type, String packageName) {
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
