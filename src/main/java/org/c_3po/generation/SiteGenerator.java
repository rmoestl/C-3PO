package org.c_3po.generation;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.c_3po.cmd.CmdArguments;
import org.c_3po.io.DirectorySynchronizer;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.*;

/**
 * Main class responsible for site generation.
 */
public class SiteGenerator {
    private TemplateEngine templateEngine;

    // Configuration Parameters
    private String sourceDirectoryPath = "";
    private String destinationDirectoryPath = "";
    private final DirectorySynchronizer directorySynchronizer;
    private final FilenameFilter htmlFilesFilter;

    private SiteGenerator(String sourceDirectoryPath, String destinationDirectoryPath) {
        templateEngine = setupTemplateEngine(sourceDirectoryPath);
        this.sourceDirectoryPath = sourceDirectoryPath;
        this.destinationDirectoryPath = destinationDirectoryPath;
        directorySynchronizer = new DirectorySynchronizer();
        htmlFilesFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        };
    }

    /**
     * Factory method that creates a SiteGenerator from command line arguments.
     */
    public static SiteGenerator fromCmdArguments(CmdArguments cmdArguments) {
        return new SiteGenerator(cmdArguments.getSourceDirectory(), cmdArguments.getDestinationDirectory());
    }

    public void generate() throws IOException {
        buildPages(sourceDirectoryPath, destinationDirectoryPath);
        syncStaticResources(sourceDirectoryPath, destinationDirectoryPath);
    }

    private void buildPages(String sourceDirectoryPath, String destinationDirectoryPath) throws FileNotFoundException {
        Context context = new Context();
        File sourceDirectory = new File(sourceDirectoryPath);
        if (sourceDirectory.isDirectory()) {
            for (File file : sourceDirectory.listFiles(htmlFilesFilter)) {
                if (!file.isDirectory()) {
                    String result = templateEngine.process(file.getName().replace(".html", ""), context);

                    PrintWriter out = new PrintWriter(destinationDirectoryPath + "/" + file.getName());
                    out.println(result);
                    out.close();
                }
            }
        }
    }

    private void syncStaticResources(String sourceDirectoryPath, String destinationDirectoryPath) throws IOException {
        directorySynchronizer.sync(sourceDirectoryPath + "/img", destinationDirectoryPath + "/img");
        directorySynchronizer.sync(sourceDirectoryPath + "/css", destinationDirectoryPath + "/css");
    }

    private TemplateEngine setupTemplateEngine(String sourceDirectoryPath) {
        TemplateResolver rootTemplateResolver = newTemplateResolver(sourceDirectoryPath);
        TemplateResolver partialsTemplateResolver = newTemplateResolver(sourceDirectoryPath + "/" + "_partials");
        TemplateResolver layoutsTemplateResolver = newTemplateResolver(sourceDirectoryPath + "/" + "_layouts");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(rootTemplateResolver);
        templateEngine.addTemplateResolver(partialsTemplateResolver);
        templateEngine.addTemplateResolver(layoutsTemplateResolver);

        templateEngine.addDialect(new LayoutDialect());

        return templateEngine;
    }

    private TemplateResolver newTemplateResolver(String prefix) {
        TemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setPrefix(prefix + "/");
        templateResolver.setSuffix(".html");
        return templateResolver;
    }

    public static void main(String[] args) {
        try {
            new SiteGenerator("../test/www", "../test/site").generate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
