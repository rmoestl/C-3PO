package org.c_3po.generation;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingStrategy;
import org.c_3po.cmd.CmdArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
    private IgnorablesMatcher ignorablesMatcher;

    private SiteGenerator(Path sourceDirectoryPath, Path destinationDirectoryPath, List<String> ignorables) {
        templateEngine = setupTemplateEngine(sourceDirectoryPath);
        this.sourceDirectoryPath = sourceDirectoryPath;
        this.destinationDirectoryPath = destinationDirectoryPath;
        this.ignorablesMatcher = IgnorablesMatcher.from(sourceDirectoryPath, ignorables);
    }

    /**
     * Factory method that creates a SiteGenerator from command line arguments.
     */
    public static SiteGenerator fromCmdArguments(CmdArguments cmdArguments) {
        Objects.requireNonNull(cmdArguments);
        Path sourceDirectoryPath = Paths.get(cmdArguments.getSourceDirectory());
        if (Files.exists(sourceDirectoryPath)) {
            return new SiteGenerator(sourceDirectoryPath,
                    Paths.get(cmdArguments.getDestinationDirectory()), readIgnorables(sourceDirectoryPath));
        } else {
            throw new IllegalArgumentException(
                    "Source directory '" + cmdArguments.getSourceDirectory() + "' does not exist.");
        }
    }

    /**
     * Does a one time site generation.
     * @throws IOException
     */
    public void generate() throws IOException {
        buildPages(sourceDirectoryPath, destinationDirectoryPath);

        // TODO Check if there are any files in destination directory that are to be ignored (e.g. because ignore file has changed since last generation)
    }

    /**
     * Does a site generation when a source file has been added, changed or deleted
     * @throws IOException
     */
    public void generateOnFileChange() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Map<WatchKey, Path> watchKeyMap = registerWatchServices(sourceDirectoryPath, watchService);

        for (;;) {
            WatchKey key;
            try {
                LOG.trace("In watcher loop waiting for a new change notification");
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
                if (Files.exists(changedPath) && Files.isSameFile(changedPath, sourceDirectoryPath.resolve(C_3PO_IGNORE_FILE_NAME))) {
                    updateIgnorables(readIgnorables(sourceDirectoryPath));
                } else {
                    Path parent = watchKeyMap.get(key);
                    changedPath = parent.resolve(changedPath);

                    if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                        if (htmlFilter.accept(changedPath)) {
                            buildPages(sourceDirectoryPath, destinationDirectoryPath);
                        } else if (staticFileFilter.accept(changedPath)) {
                            Path parentDir = sourceDirectoryPath.relativize((Path) key.watchable());
                            buildPages(parentDir, destinationDirectoryPath.resolve(parentDir));
                        } else if (Files.isDirectory(changedPath) && !isIgnorablePath(changedPath)) {
                            if (kind == ENTRY_CREATE) {
                                watchKeyMap.put(registerWatchService(watchService, changedPath), changedPath);
                                LOG.debug("Registered autoBuild watcher for '{}", changedPath);
                            }
                            buildPages(sourceDirectoryPath, destinationDirectoryPath);
                        }

                    } else if (kind == ENTRY_DELETE) {
                        if (!isIgnorablePath(changedPath)) {
                            Path targetPath = destinationDirectoryPath.resolve(changedPath);

                            // Delete files and directories in target directory
                            if (Files.exists(targetPath)) {
                                if (Files.isDirectory(targetPath)) {
                                    deleteDirectory(targetPath);
                                } else {
                                    Files.deleteIfExists(destinationDirectoryPath.resolve(changedPath));
                                }
                            }

                            // Cancel watcher for the path if there was one registered
                            if (watchKeyMap.get(key).equals(changedPath)) {
                                key.cancel();
                                watchKeyMap.remove(key);
                                LOG.debug("Cancelled autoBuild watcher for '{}", changedPath);
                            }
                        }
                    }
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

    private Map<WatchKey, Path> registerWatchServices(Path rootDirectory, WatchService watchService) throws IOException {
        Map<WatchKey, Path> watchKeyMap = new HashMap<>();

        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!isIgnorablePath(Objects.requireNonNull(dir).normalize())) {
                    watchKeyMap.put(registerWatchService(watchService, dir), dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        });

        watchKeyMap.values().stream().forEach(path -> LOG.debug("Registered autoBuild watcher for '{}", path));
        return watchKeyMap;
    }

    private WatchKey registerWatchService(WatchService watchService, Path pathToWatch) throws IOException {
        if (Files.exists(pathToWatch)) {
            return pathToWatch.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } else {
            throw new FileNotFoundException(String.format("Path '%s' to watch does not exist", pathToWatch));
        }
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
                         Files.newDirectoryStream(sourceDir,
                                 entry -> Files.isDirectory(entry) && !isIgnorablePath(entry.normalize()))) {
                for (Path subDir : subDirStream) {
                    LOG.trace("I'm going to build pages in this subdirectory [{}]", subDir);
                    buildPages(subDir, targetDir.resolve(subDir.getFileName()));
                }
            }
        }
    }

    private TemplateEngine setupTemplateEngine(Path sourceDirectoryPath) {
        TemplateEngine templateEngine = new TemplateEngine();

        // Note: we need two FileTemplateResolvers
        // one that is able to deal with absolute path template names like 'D:/data/dev/blog/index'
        // and one that is able to resolve relative path template names like '_layouts/main-layout'
        templateEngine.addTemplateResolver(newTemplateResolver(sourceDirectoryPath.toAbsolutePath()));
        templateEngine.addTemplateResolver(newTemplateResolver());
        templateEngine.addDialect(new LayoutDialect(new GroupingStrategy()));
        return templateEngine;
    }

    private TemplateResolver newTemplateResolver() {
        return newTemplateResolver(null);
    }

    private TemplateResolver newTemplateResolver(Path prefix) {
        TemplateResolver templateResolver = new FileTemplateResolver();

        // Instead of 'HTML5' this template mode allows void elements such as meta to have no closing tags
        templateResolver.setTemplateMode("LEGACYHTML5");
        templateResolver.setPrefix(prefix != null ? prefix.toString() + "/" : "");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    private static List<String> readIgnorables(Path baseDirectory) {
        return Ignorables.read(baseDirectory.resolve(C_3PO_IGNORE_FILE_NAME));
    }

    private boolean isIgnorablePath(Path path) throws IOException {
        return ignorablesMatcher.matches(path)
                || Files.exists(destinationDirectoryPath) && Files.exists(path) && Files.isSameFile(path, destinationDirectoryPath);
    }

    private void updateIgnorables(List<String> newIgnorables) {
        List<String> addedIgnorables = new ArrayList<>(newIgnorables);
        addedIgnorables.removeAll(this.ignorablesMatcher.getGlobPatterns());
        try {
            if (!addedIgnorables.isEmpty() && Files.exists(destinationDirectoryPath)) {
                LOG.debug("Removing added ignorables '{}' after ignorables update", addedIgnorables);
                removeIgnorables(addedIgnorables, destinationDirectoryPath);
            }
        } catch (IOException e) {
            LOG.error("IO error occurred when removing ignored files from target directory '{}'", destinationDirectoryPath, e);
        }

        // Note: No action needed when file patterns have been **removed** from newIgnorables since
        // these should be included when c-3po processes the site the next time

        this.ignorablesMatcher = IgnorablesMatcher.from(sourceDirectoryPath, newIgnorables);
    }

    /**
     * Removes the files and directories defined by ignorables from withing the given rootDirectory.
     *
     * @param ignorables a list of glob patterns defining the files and directories to remove
     * @param rootDirectory the root directory
     * @throws IOException
     */
    private void removeIgnorables(List<String> ignorables, Path rootDirectory) throws IOException {
        final IgnorablesMatcher ignorablesMatcher = IgnorablesMatcher.from(rootDirectory, ignorables);

        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (ignorablesMatcher.matches(rootDirectory.relativize(dir).normalize())) {
                    LOG.debug("Deleting directory '{}'", dir);
                    deleteDirectory(dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (ignorablesMatcher.matches(rootDirectory.relativize(file).normalize())) {
                    LOG.debug("Deleting file '{}'", file);
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }
}
