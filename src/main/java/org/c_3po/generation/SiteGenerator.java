package org.c_3po.generation;

import org.c_3po.cmd.CmdArguments;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Main class responsible for site generation.
 */
public class SiteGenerator {
    private TemplateEngine templateEngine;

    // Configuration Parameters
    private String sourceDirectoryPath = "";
    private String destinationDirectoryPath = "";

    private SiteGenerator(String sourceDirectoryPath, String destinationDirectoryPath) {
        templateEngine = setupTemplateEngine(sourceDirectoryPath);
        this.sourceDirectoryPath = sourceDirectoryPath;
        this.destinationDirectoryPath = destinationDirectoryPath;
    }

    /**
     * Factory method that creates a SiteGenerator from command line arguments.
     */
    public static SiteGenerator fromCmdArguments(CmdArguments cmdArguments) {
        return new SiteGenerator(cmdArguments.getSourceDirectory(), cmdArguments.getDestinationDirectory());
    }

    public void generate() throws FileNotFoundException {
        Context context = new Context();

        File sourceDirectory = new File(sourceDirectoryPath);
        if (sourceDirectory.isDirectory()) {
            for (File file : sourceDirectory.listFiles()) {
                if (!file.isDirectory()) {
                    String result = templateEngine.process(file.getName().replace(".html", ""), context);

                    PrintWriter out = new PrintWriter(destinationDirectoryPath + "/" + file.getName());
                    out.println(result);
                    out.close();
                }
            }
        }
    }

    private TemplateEngine setupTemplateEngine(String sourceDirectoryName) {
        TemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setPrefix(sourceDirectoryName + "/");
        templateResolver.setSuffix(".html");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}
