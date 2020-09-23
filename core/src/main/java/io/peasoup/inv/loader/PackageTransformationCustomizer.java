package io.peasoup.inv.loader;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

public class PackageTransformationCustomizer extends CompilationCustomizer {

    public PackageTransformationCustomizer() {
        super(CompilePhase.CONVERSION);
    }

    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        ModuleNode ast = source.getAST();
        String desc = ast.getDescription();

        // If it does not have a package-like name, skip
        if (!desc.contains("."))
            return;

        String newPackage = desc.substring(0, desc.lastIndexOf("."));
        String newName = desc.substring(desc.lastIndexOf(".") + 1);
        ast.addStarImport(newPackage + ".");

        // Do not set package name to script object
        if (classNode.getSuperClass().getName().equalsIgnoreCase("groovy.lang.Script")) {
            ast.setPackageName("");
            classNode.setName(newName);
        } else {
            ast.setPackageName(newPackage);
            classNode.setName(newPackage + "." + classNode.getName());
        }
    }
}
