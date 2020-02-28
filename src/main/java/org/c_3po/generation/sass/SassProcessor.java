package org.c_3po.generation.sass;

import io.bit3.jsass.*;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.context.FileContext;
import io.bit3.jsass.context.StringContext;

import java.nio.file.Path;

/**
 * Responsible for compiling SASS files to CSS files.
 */
public class SassProcessor {

    private SassProcessor() {
    }

    public static SassProcessor getInstance() {
        return new SassProcessor();
    }

    public String process(Path sassFile) throws CompilationException {
        Compiler compiler = new Compiler();
        Options options = new Options();
        options.setOutputStyle(OutputStyle.COMPRESSED);
        FileContext fileContext = new FileContext(sassFile.toUri(), null, options);
        Output output = compiler.compile(fileContext);
        return output.getCss();
    }
}
