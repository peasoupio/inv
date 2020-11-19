package io.peasoup.inv.loader;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

public class PackageTransformationCustomizer extends CompilationCustomizer {

    public PackageTransformationCustomizer() {
        super(CompilePhase.CONVERSION);
    }

    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        if (!(source.getClassLoader() instanceof ExtGroovyClassLoader))
            return;

        ExtGroovyClassLoader egcl = (ExtGroovyClassLoader)source.getClassLoader();
        ExtGroovyClassLoader.ExtConfig cfg = egcl.getExtConfig(context.getCompileUnit().getCodeSource());

        cfg.updateClassnode(source.getAST(), classNode);
    }
}
