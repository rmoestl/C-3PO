package org.c_3po.generation;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.c_3po.cmd.CmdArguments;
import org.c_3po.io.DirectorySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Main class responsible for site generation.
 */
public class SiteGenerator {
    public static final String STD_DIR_LAYOUTS = "_layouts";
    public static final String STD_DIR_PARTIALS = "_partials";
    public static final String STD_DIR_IMG = "img";
    public static final String STD_DIR_CSS = "css";
    public static final String STD_DIR_JS = "js";

    private static final Logger LOG = LoggerFactory.getLogger(SiteGenerator.class);
    public static final String HTACCESS = ".htaccess";
    public static final String FAVICON_ICO = "favicon.ico";
    public static final String HUMANS_TXT = "humans.txt";
    public static final String ROBOTS_TXT = "robots.txt";

    private final DirectorySynchronizer directorySynchronizer;
    private final FilenameFilter htmlFilesFilter;
    private final String sourceDirectoryPath;
    private final String destinationDirectoryPath;

    private TemplateEngine templateEngine;

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
        Objects.requireNonNull(cmdArguments);
        if (!Files.exists(Paths.get(cmdArguments.getSourceDirectory()))) {
            throw new IllegalArgumentException(
                    "Source directory '" + cmdArguments.getSourceDirectory() + "' does not exist.");
        } else {
            return new SiteGenerator(cmdArguments.getSourceDirectory(), cmdArguments.getDestinationDirectory());
        }
    }

    /**
     * Does a one time site generation.
     * @throws IOException
     */
    public void generate() throws IOException {
        processPages(Paths.get(sourceDirectoryPath), Paths.get(destinationDirectoryPath));
        processStaticResources(sourceDirectoryPath, destinationDirectoryPath);
    }

    /**
     * Does a site generation when a source file has been added, changed or deleted
     * @throws IOException
     */
    public void generateOnFileChange() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        WatchKey cssWatchKey = registerToWatchService(watchService, Paths.get(sourceDirectoryPath, STD_DIR_CSS));
        WatchKey jsWatchKey = registerToWatchService(watchService, Paths.get(sourceDirectoryPath, STD_DIR_JS));
        WatchKey imgWatchKey = registerToWatchService(watchService, Paths.get(sourceDirectoryPath, STD_DIR_IMG));
        WatchKey htmlWatchKey = registerToWatchService(watchService, Paths.get(sourceDirectoryPath));
        WatchKey layoutsWatchKey = registerToWatchService(watchService, Paths.get(sourceDirectoryPath, STD_DIR_LAYOUTS));
        WatchKey partialsWatchKey = registerToWatchService(watchService, Paths.get(sourceDirectoryPath, STD_DIR_PARTIALS));

        for (;;) {
            LOG.debug("In watcher loop");
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException ex) {
                return; // stops the infinite loop
            }

            // Now that we have a "signaled" (as opposed to "ready" and "invalid") watch key,
            // let's see what's in there for us
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
                } else if (key == htmlWatchKey || key == layoutsWatchKey || key == partialsWatchKey) {
                    templateEngine.clearTemplateCache();
                    buildPages(Paths.get(sourceDirectoryPath), Paths.get(destinationDirectoryPath));
                } else if (key == jsWatchKey) {
                    processStaticResources(Paths.get(sourceDirectoryPath, STD_DIR_JS),
                            Paths.get(destinationDirectoryPath, STD_DIR_JS));
                } else if (key == imgWatchKey) {
                    processStaticResources(Paths.get(sourceDirectoryPath, STD_DIR_IMG),
                            Paths.get(destinationDirectoryPath, STD_DIR_IMG));
                }

                // Reset the key -- this step is critical if you want to
                // receive further watch events. If the key is no longer valid,
                // the directory is inaccessible, so exit the loop.
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }
    }

    private WatchKey registerToWatchService(WatchService watchService, Path pathToWatch) throws IOException {
        return pathToWatch.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }

    private void processPages(Path sourceDirectory, Path targetDirectory) throws IOException {
        processPageDirectory(sourceDirectory, targetDirectory);
    }

    private void processPageDirectory(Path sourceDirectory, Path targetDirectory) throws IOException {
        buildPages(sourceDirectory, targetDirectory);
        synchronizeStandardServerFiles(sourceDirectory, targetDirectory);
    }

    private void buildPages(Path sourceDir, Path targetDir) throws IOException {
        Context context = new Context();
        File sourceDirectoryFile = sourceDir.toFile();
        if (sourceDirectoryFile.isDirectory()) {
            for (File file : sourceDirectoryFile.listFiles(htmlFilesFilter)) {
                if (!file.isDirectory()) {
                    List<String> lines = Arrays.asList(templateEngine.process(file.getName().replace(".html", ""), context));
                    Path destinationPath = targetDir.resolve(file.getName());
                    Files.write(destinationPath, lines, Charset.forName("UTF-8"), CREATE, WRITE);
                }
            }
        }
    }

    private void synchronizeStandardServerFiles(Path sourceDir, Path targetDir) throws IOException {
        Files.copy(sourceDir.resolve(HTACCESS), targetDir.resolve(HTACCESS));
        Files.copy(sourceDir.resolve(FAVICON_ICO), targetDir.resolve(FAVICON_ICO));
        Files.copy(sourceDir.resolve(HUMANS_TXT), targetDir.resolve(HUMANS_TXT));
        Files.copy(sourceDir.resolve(ROBOTS_TXT), targetDir.resolve(ROBOTS_TXT));
    }

    private void processStaticResources(String sourceDirectoryPath, String destinationDirectoryPath) throws IOException {
        syncCssResources(sourceDirectoryPath, destinationDirectoryPath);
        directorySynchronizer.sync(path(sourceDirectoryPath, STD_DIR_IMG), path(destinationDirectoryPath, STD_DIR_IMG));
        directorySynchronizer.sync(path(sourceDirectoryPath, STD_DIR_JS), path(destinationDirectoryPath, STD_DIR_JS));
    }

    private void processStaticResources(Path source, Path destination) throws IOException {
        directorySynchronizer.sync(source.toAbsolutePath().toString(), destination.toString());
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

        // Instead of 'HTML5' this template mode allows void elements such as meta to have no closing tags
        templateResolver.setTemplateMode("LEGACYHTML5");
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
