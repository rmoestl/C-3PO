package org.c_3po.generation;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.c_3po.cmd.CmdArguments;
import org.c_3po.io.DirectorySynchronizer;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.*;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Main class responsible for site generation.
 */
public class SiteGenerator {
    public static final String STD_DIR_IMG = "img";
    public static final String STD_DIR_CSS = "css";
    public static final String STD_DIR_JS = "js";
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

    /**
     * Does a one time site generation.
     * @throws IOException
     */
    public void generate() throws IOException {
        buildPages(sourceDirectoryPath, destinationDirectoryPath);
        syncStaticResources(sourceDirectoryPath, destinationDirectoryPath);
    }

    /**
     * Does a site generation when a source file has been added, changed or deleted
     * @throws IOException
     */
    public void generateOnFileChange() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        WatchKey cssWatchKey = Paths.get(path(sourceDirectoryPath, STD_DIR_CSS))
                .register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        for (;;) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException ex) {
                return; // stops the infinite loop
            }

            // Now that we have a "signaled" (as opposed to "ready" and "invalid") watch key,
            // let's what's in there for us
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Ignore the overflow event, that can happen always - i.e. it does
                // not have to be registered with the watcher
                if (kind == OVERFLOW) {
                    continue;
                }

                // TODO What if file changes interfere?

                // TODO Add behaviour for the other folders
                if (key == cssWatchKey) {
                    syncCssResources(sourceDirectoryPath, destinationDirectoryPath);
                }

                // Reset the key -- this step is critical if you want to
                // receive further watch events.  If the key is no longer valid,
                // the directory is inaccessible so exit the loop.
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }

    }

    private void buildPages(String sourceDirectoryPath, String destinationDirectoryPath) throws FileNotFoundException {
        Context context = new Context();
        File sourceDirectory = new File(sourceDirectoryPath);
        if (sourceDirectory.isDirectory()) {
            for (File file : sourceDirectory.listFiles(htmlFilesFilter)) {
                if (!file.isDirectory()) {
                    String result = templateEngine.process(file.getName().replace(".html", ""), context);

                    // TODO Apply try-with-resources
                    PrintWriter out = new PrintWriter(path(destinationDirectoryPath, file.getName()));
                    out.println(result);
                    out.close();
                }
            }
        }
    }

    private void syncStaticResources(String sourceDirectoryPath, String destinationDirectoryPath) throws IOException {
        syncCssResources(sourceDirectoryPath, destinationDirectoryPath);
        directorySynchronizer.sync(path(sourceDirectoryPath, STD_DIR_IMG), path(destinationDirectoryPath, STD_DIR_IMG));
        directorySynchronizer.sync(path(sourceDirectoryPath, STD_DIR_JS), path(destinationDirectoryPath, STD_DIR_JS));
    }

    private void syncCssResources(String sourceDirectoryPath, String destinationDirectoryPath) throws IOException {
        directorySynchronizer.sync(path(sourceDirectoryPath, STD_DIR_CSS), path(destinationDirectoryPath, STD_DIR_CSS));
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
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    private String path(String first, String... more) {
        return Paths.get(first, more).toString();
    }

    public static void main(String[] args) {
        try {
            new SiteGenerator("../test/www", "../test/site").generate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
