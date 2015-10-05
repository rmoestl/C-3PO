package org.c_3po.generation;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.c_3po.cmd.CmdArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Main class responsible for site generation.
 */
public class SiteGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SiteGenerator.class);
    private static final Context DEFAULT_THYMELEAF_CONTEXT = new Context();
    private static final String C_3PO_IGNORE_FILE_NAME = ".c3poignore";

    private final Path sourceDirectoryPath;
    private final Path destinationDirectoryPath;
    private final DirectoryStream.Filter<Path> htmlFilter =
            entry -> Files.isRegularFile(entry) && !isIgnorablePath(entry) && entry.toFile().getName().endsWith(".html");
    private final DirectoryStream.Filter<Path> staticFileFilter =
            entry -> Files.isRegularFile(entry) && !isIgnorablePath(entry) && !htmlFilter.accept(entry);

    private TemplateEngine templateEngine;
    private List<Path> ignorables;

    private SiteGenerator(Path sourceDirectoryPath, Path destinationDirectoryPath, List<Path> ignorables) {
        templateEngine = setupTemplateEngine(sourceDirectoryPath);
        this.sourceDirectoryPath = sourceDirectoryPath;
        this.destinationDirectoryPath = destinationDirectoryPath;
        this.setIgnorables(ignorables);
    }

    /**
     * Factory method that creates a SiteGenerator from command line arguments.
     */
    public static SiteGenerator fromCmdArguments(CmdArguments cmdArguments) {
        Objects.requireNonNull(cmdArguments);
        Path sourceDirectoryPath = Paths.get(cmdArguments.getSourceDirectory());
        if (!Files.exists(sourceDirectoryPath)) {
            throw new IllegalArgumentException(
                    "Source directory '" + cmdArguments.getSourceDirectory() + "' does not exist.");
        } else {
            return new SiteGenerator(sourceDirectoryPath,
                    Paths.get(cmdArguments.getDestinationDirectory()), readIgnorablesFromFile(sourceDirectoryPath));
        }
    }

    /**
     * Does a one time site generation.
     * @throws IOException
     */
    public void generate() throws IOException {
        buildPages(sourceDirectoryPath, destinationDirectoryPath);
    }

    /**
     * Does a site generation when a source file has been added, changed or deleted
     * @throws IOException
     */
    public void generateOnFileChange() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        registerToWatchService(watchService, sourceDirectoryPath);
        // TODO Watch for .c3poignore file changes

        for (;;) {
            WatchKey key;
            try {
                LOG.debug("In watcher loop waiting for a new change notification");
                key = watchService.take();
            } catch (InterruptedException ex) {
                return; // stops the infinite loop
            }

            // Now that we have a "signaled" (as opposed to "ready" and "invalid") watch key,
            // let's see what's in there for us
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                LOG.debug("File '{}' with kind '{}' triggered a change", event.context(), event.kind());

                // Ignore the overflow event, that can happen always - i.e. it does
                // not have to be registered with the watcher
                if (kind == OVERFLOW) {
                    continue;
                }

                // Depending on type of resource let's build the whole site or just a portion
                Path changedPath = (Path) event.context();
                if (Files.isDirectory(changedPath) && !isIgnorablePath(changedPath) || htmlFilter.accept(changedPath)) {
                    buildPages(sourceDirectoryPath, destinationDirectoryPath);
                } else if (staticFileFilter.accept(changedPath)) {
                    Path parentDir = sourceDirectoryPath.relativize((Path) key.watchable());
                    buildPages(parentDir, destinationDirectoryPath.resolve(parentDir));
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

    private void buildPages(Path sourceDir, Path targetDir) throws IOException {
        if (Files.exists(sourceDir) && Files.isDirectory(sourceDir)) {
            LOG.debug("Building pages contained in '{}'", sourceDir);

            // Clear Thymeleaf's template cache
            templateEngine.clearTemplateCache();

            // Ensure targetDir exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Look for HTML files to generate
            try (DirectoryStream<Path> htmlFilesStream = Files.newDirectoryStream(sourceDir, htmlFilter)) {
                for (Path htmlFile : htmlFilesStream) {
                    LOG.trace("Generate '{}'", htmlFile);

                    // Generate
                    List<String> lines = Collections.singletonList(
                            templateEngine.process(htmlFile.toString().replace(".html", ""), DEFAULT_THYMELEAF_CONTEXT));

                    // Write to file
                    Path destinationPath = targetDir.resolve(htmlFile.getFileName());
                    try {
                        Files.write(destinationPath, lines, Charset.forName("UTF-8"), CREATE, WRITE, TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        LOG.error("Failed to write generated document to {}", destinationPath, e);
                    }
                }
            }

            // Look for static files to synchronize
            try (DirectoryStream<Path> staticFilesStream = Files.newDirectoryStream(sourceDir, staticFileFilter)) {
                for (Path staticFile : staticFilesStream) {
                    Files.copy(staticFile, targetDir.resolve(staticFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Look for subdirectories that are to be processed by c-3po
            try (DirectoryStream<Path> subDirStream =
                         Files.newDirectoryStream(sourceDir, entry -> Files.isDirectory(entry) && !isIgnorablePath(entry))) {
                for (Path subDir : subDirStream) {
                    LOG.trace("I'm going to build pages in this subdirectory [{}]", subDir);
                    buildPages(subDir, targetDir.resolve(subDir.getFileName()));
                }
            }
        }
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

    private static List<Path> readIgnorablesFromFile(Path baseDirectory) {
        ArrayList<Path> ingnorablePaths = new ArrayList<>();

        Path c3poIgnoreFile = baseDirectory.resolve(C_3PO_IGNORE_FILE_NAME);
        if (Files.exists(c3poIgnoreFile)) {
            if (Files.isRegularFile(c3poIgnoreFile) && Files.isReadable(c3poIgnoreFile)) {
                try {
                    ingnorablePaths.addAll(Files.readAllLines(c3poIgnoreFile).stream()
                            .map(baseDirectory::resolve)
                            .collect(Collectors.toList()));
                    ingnorablePaths.add(c3poIgnoreFile);
                    LOG.info("'{}' read successfully", C_3PO_IGNORE_FILE_NAME);
                } catch (IOException e) {
                    LOG.error("Failed to read '{}' from '{}'. No files and directories will be ignored by C-3PO " +
                            "during processing", C_3PO_IGNORE_FILE_NAME, c3poIgnoreFile, e);
                }
            } else {
                LOG.info("Invalid '{}' file found. Make sure it's a regular file and readable", C_3PO_IGNORE_FILE_NAME);
            }
        } else {
            LOG.info("No '{}' file detected in directory '{}'. " +
                    "In a '{}' file you can exclude files and folders from being processed by C-3PO",
                    C_3PO_IGNORE_FILE_NAME, baseDirectory, C_3PO_IGNORE_FILE_NAME);
        }

        return ingnorablePaths;
    }

    private boolean isIgnorablePath(Path path) throws IOException {
        return Files.isSameFile(path, destinationDirectoryPath) || ignorables.contains(path);
    }

    public void setIgnorables(List<Path> ignorables) {
        if (this.ignorables == null) {
            this.ignorables = ignorables;
        } else {
            // TODO diff ignorables and update target dir (edge cases)
        }
    }
}
