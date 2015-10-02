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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Main class responsible for site generation.
 */
public class SiteGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SiteGenerator.class);
    private static final String STD_DIR_LAYOUTS = "_layouts";
    private static final String STD_DIR_PARTIALS = "_partials";
    private static final String STD_DIR_IMG = "img";
    private static final String STD_DIR_CSS = "css";
    private static final String STD_DIR_JS = "js";
    private static final String STD_DIR_GIT = ".git";
    private static final String HTACCESS = ".htaccess";
    private static final String FAVICON_ICO = "favicon.ico";
    private static final String HUMANS_TXT = "humans.txt";
    private static final String ROBOTS_TXT = "robots.txt";
    private static final String SITEMAP_TXT = "sitemap.txt";
    private static final Context DEFAULT_THYMELEAF_CONTEXT = new Context();

    private final DirectorySynchronizer directorySynchronizer;
    private final Path sourceDirectoryPath;
    private final Path destinationDirectoryPath;

    private TemplateEngine templateEngine;

    private SiteGenerator(Path sourceDirectoryPath, Path destinationDirectoryPath) {
        templateEngine = setupTemplateEngine(sourceDirectoryPath);
        this.sourceDirectoryPath = sourceDirectoryPath;
        this.destinationDirectoryPath = destinationDirectoryPath;
        directorySynchronizer = new DirectorySynchronizer();
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
            return new SiteGenerator(Paths.get(cmdArguments.getSourceDirectory()),
                    Paths.get(cmdArguments.getDestinationDirectory()));
        }
    }

    /**
     * Does a one time site generation.
     * @throws IOException
     */
    public void generate() throws IOException {
        processPages(sourceDirectoryPath, destinationDirectoryPath);
        processAllStaticResources(sourceDirectoryPath, destinationDirectoryPath);
    }

    /**
     * Does a site generation when a source file has been added, changed or deleted
     * @throws IOException
     */
    public void generateOnFileChange() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        WatchKey cssWatchKey = registerToWatchService(watchService, sourceDirectoryPath.resolve(STD_DIR_CSS));
        WatchKey jsWatchKey = registerToWatchService(watchService, sourceDirectoryPath.resolve(STD_DIR_JS));
        WatchKey imgWatchKey = registerToWatchService(watchService, sourceDirectoryPath.resolve(STD_DIR_IMG));
        WatchKey htmlWatchKey = registerToWatchService(watchService, sourceDirectoryPath);
        WatchKey layoutsWatchKey = registerToWatchService(watchService, sourceDirectoryPath.resolve(STD_DIR_LAYOUTS));
        WatchKey partialsWatchKey = registerToWatchService(watchService, sourceDirectoryPath.resolve(STD_DIR_PARTIALS));

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

                if (key == cssWatchKey) {
                    processStaticResources(sourceDirectoryPath.resolve(STD_DIR_CSS),
                            destinationDirectoryPath.resolve(STD_DIR_CSS));
                } else if (key == htmlWatchKey || key == layoutsWatchKey || key == partialsWatchKey) {
                    templateEngine.clearTemplateCache();
                    buildPages(sourceDirectoryPath, destinationDirectoryPath);
                } else if (key == jsWatchKey) {
                    processStaticResources(sourceDirectoryPath.resolve(STD_DIR_JS),
                            destinationDirectoryPath.resolve(STD_DIR_JS));
                } else if (key == imgWatchKey) {
                    processStaticResources(sourceDirectoryPath.resolve(STD_DIR_IMG),
                            destinationDirectoryPath.resolve(STD_DIR_IMG));
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
        return Files.exists(pathToWatch)
                ? pathToWatch.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                : null;
    }

    private void processPages(Path sourceDirectory, Path targetDirectory) throws IOException {
        processPageDirectory(sourceDirectory, targetDirectory);
    }

    private void processPageDirectory(Path sourceDirectory, Path targetDirectory) throws IOException {
        buildPages(sourceDirectory, targetDirectory);
        syncStandardServerFiles(sourceDirectory, targetDirectory);
    }

    private void buildPages(Path sourceDir, Path targetDir) throws IOException {
        if (Files.exists(sourceDir) && Files.isDirectory(sourceDir)) {

            // Ensure targetDir exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Look for HTML files to generate
            try (DirectoryStream<Path> htmlFilesStream =
                         Files.newDirectoryStream(sourceDir, entry -> Files.isRegularFile(entry)
                                 && entry.toFile().getName().endsWith(".html"))) {
                for (Path htmlFile : htmlFilesStream) {

                    // Generate
                    Path relativeFilePath = sourceDirectoryPath.relativize(htmlFile);
                    List<String> lines = Collections.singletonList(
                            templateEngine.process(relativeFilePath.toString().replace(".html", ""), DEFAULT_THYMELEAF_CONTEXT));

                    // Write to file
                    Path destinationPath = targetDir.resolve(htmlFile.getFileName());
                    try {
                        Files.write(destinationPath, lines, Charset.forName("UTF-8"), CREATE, WRITE, TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        LOG.error("Failed to write generated document to {}", destinationPath, e);
                    }
                }
            }

            // Look for subdirectories that are to be processed by c-3po
            try (DirectoryStream<Path> subDirStream =
                         Files.newDirectoryStream(sourceDir, entry -> Files.isDirectory(entry)
                                 && !isSpecialDir(entry)
                                 && !Files.isSameFile(entry, destinationDirectoryPath))) {
                for (Path subDir : subDirStream) {
                    LOG.info("I'm going to build pages in this subdirectory [{}]", subDir);
                    buildPages(subDir, targetDir.resolve(subDir.getFileName()));
                }
            }
        }
    }

    private void syncStandardServerFiles(Path sourceDir, Path targetDir) throws IOException {
        syncFile(sourceDir.resolve(HTACCESS), targetDir);
        syncFile(sourceDir.resolve(FAVICON_ICO), targetDir);
        syncFile(sourceDir.resolve(HUMANS_TXT), targetDir);
        syncFile(sourceDir.resolve(ROBOTS_TXT), targetDir);
        syncFile(sourceDir.resolve(SITEMAP_TXT), targetDir);
    }

    private void syncFile(Path file, Path targetDir) throws IOException {
        if (file.toFile().exists()) {
            Files.copy(file, targetDir.resolve(file.getFileName()), REPLACE_EXISTING);
        }
    }

    private void processAllStaticResources(Path sourceDirectoryPath, Path destinationDirectoryPath) throws IOException {
        directorySynchronizer.sync(sourceDirectoryPath.resolve(STD_DIR_CSS), destinationDirectoryPath.resolve(STD_DIR_CSS));
        directorySynchronizer.sync(sourceDirectoryPath.resolve(STD_DIR_IMG), destinationDirectoryPath.resolve(STD_DIR_IMG));
        directorySynchronizer.sync(sourceDirectoryPath.resolve(STD_DIR_JS), destinationDirectoryPath.resolve(STD_DIR_JS));
    }

    private void processStaticResources(Path source, Path destination) throws IOException {
        directorySynchronizer.sync(source, destination);
    }

    private TemplateEngine setupTemplateEngine(Path sourceDirectoryPath) {
        TemplateResolver rootTemplateResolver = newTemplateResolver(sourceDirectoryPath);
        TemplateResolver partialsTemplateResolver = newTemplateResolver(sourceDirectoryPath.resolve("/_partials"));
        TemplateResolver layoutsTemplateResolver = newTemplateResolver(sourceDirectoryPath.resolve("/_layouts"));

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(rootTemplateResolver);
        templateEngine.addTemplateResolver(partialsTemplateResolver);
        templateEngine.addTemplateResolver(layoutsTemplateResolver);

        templateEngine.addDialect(new LayoutDialect());

        return templateEngine;
    }

    private TemplateResolver newTemplateResolver(Path prefix) {
        TemplateResolver templateResolver = new FileTemplateResolver();

        // Instead of 'HTML5' this template mode allows void elements such as meta to have no closing tags
        templateResolver.setTemplateMode("LEGACYHTML5");
        templateResolver.setPrefix(prefix.toString() + "/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    private boolean isSpecialDir(Path dir) {
        return dir.endsWith(STD_DIR_LAYOUTS) ||
                dir.endsWith(STD_DIR_PARTIALS) ||
                dir.endsWith(STD_DIR_CSS) ||
                dir.endsWith(STD_DIR_JS) ||
                dir.endsWith(STD_DIR_IMG) ||
                dir.endsWith(STD_DIR_GIT);
    }
}
