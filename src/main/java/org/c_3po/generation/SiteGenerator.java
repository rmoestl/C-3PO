package org.c_3po.generation;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.c_3po.cmd.CmdArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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
    private List<Ignorable> ignorables;

    private SiteGenerator(Path sourceDirectoryPath, Path destinationDirectoryPath, List<Ignorable> ignorables) {
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
                    setIgnorables(readIgnorablesFromFile(sourceDirectoryPath));
                } else {
                    Path parent = watchKeyMap.get(key);
                    changedPath = parent.resolve(changedPath);
                    if (htmlFilter.accept(changedPath)) {
                        buildPages(sourceDirectoryPath, destinationDirectoryPath);
                    } else if (Files.isDirectory(changedPath) && !isIgnorablePath(changedPath)) {
                        if (kind == ENTRY_CREATE) {
                            watchKeyMap.put(registerWatchService(watchService, changedPath), changedPath);
                            LOG.debug("Registered autoBuild watcher for '{}", changedPath);
                        } else if (kind == ENTRY_DELETE) {
                            key.cancel();
                            watchKeyMap.remove(key);
                            LOG.debug("Cancelled autoBuild watcher for '{}", changedPath);
                        }
                        buildPages(sourceDirectoryPath, destinationDirectoryPath);
                    } else if (staticFileFilter.accept(changedPath)) {
                        Path parentDir = sourceDirectoryPath.relativize((Path) key.watchable());
                        buildPages(parentDir, destinationDirectoryPath.resolve(parentDir));
                    }
                    // TODO handle when path has been deleted: also delete it in target directory
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
                Objects.requireNonNull(dir);
                Objects.requireNonNull(attrs);
                if (!isIgnorablePath(dir)) {
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

    private static List<Ignorable> readIgnorablesFromFile(Path baseDirectory) {
        ArrayList<Ignorable> ingnorablePaths = new ArrayList<>();

        Path c3poIgnoreFile = baseDirectory.resolve(C_3PO_IGNORE_FILE_NAME);
        if (Files.exists(c3poIgnoreFile)) {
            if (Files.isRegularFile(c3poIgnoreFile) && Files.isReadable(c3poIgnoreFile)) {
                try {
                    ingnorablePaths.addAll(Files.readAllLines(c3poIgnoreFile).stream()
                            .map(Ignorable::new)
                            .collect(Collectors.toList()));
                    ingnorablePaths.add(new Ignorable(C_3PO_IGNORE_FILE_NAME));
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

        // Note about performance: running through the list of PathMatchers and evaluating
        // each of them for each path might be a performance bottleneck. A more efficient
        // solution might be to only generate path matchers for true GLOB strings read in from
        // .c3poignore. For non-GLOB strings a simple list of Path objects could be maintained with which
        // comparison is done through its .contains method.
        return ignorables.stream().anyMatch(ignorable -> ignorable.toPathMatcher().matches(path.normalize()))
                || Files.exists(destinationDirectoryPath) && Files.isSameFile(path, destinationDirectoryPath);
    }

    private void setIgnorables(List<Ignorable> ignorables) {
        List<Ignorable> addedIgnorables = new ArrayList<>(ignorables);

        if (this.ignorables != null) {
            addedIgnorables.removeAll(this.ignorables);
        }

        try {
            if (Files.exists(destinationDirectoryPath)) {
                removeIgnorables(addedIgnorables, destinationDirectoryPath);
            }
        } catch (IOException e) {
            LOG.error("IO error occurred when removing ignored files from target directory '{}'", destinationDirectoryPath, e);
        }

        // Note: No action needed when file patterns have been **removed** from ignorables since
        // these should be included when c-3po processes the site the next time

        this.ignorables = new ArrayList<>(ignorables);
    }

    private void removeIgnorables(List<Ignorable> ignorables, Path rootDirectory) throws IOException {
        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                for (Ignorable ignorable : ignorables) {
                    if (ignorable.toPathMatcher(rootDirectory.toString()).matches(dir)) {
                        LOG.debug("Deleting directory '{}'", dir);
                        deleteDirectory(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                for (Ignorable ignorable : ignorables) {
                    if (ignorable.toPathMatcher(rootDirectory.toString()).matches(file)) {
                        LOG.debug("Deleting file '{}'", file);
                        Files.delete(file);
                    }
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

    /**
     * Helper class representing an ignorable file or directory.
     */
    private static class Ignorable {
        private final String globPattern;
        private final Map<String, PathMatcher> cache = new HashMap<>(2);

        Ignorable(String globPattern) {
            this.globPattern = globPattern;
        }

        PathMatcher toPathMatcher(String prefix) {
            Objects.requireNonNull(prefix);
            if (cache.containsKey(prefix)) {
                return cache.get(prefix);
            } else {
                PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + ("".equals(prefix) ? "" : prefix + "/") + globPattern);
                cache.put(prefix, pathMatcher);
                return pathMatcher;
            }
        }

        PathMatcher toPathMatcher() {
            return toPathMatcher("");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ignorable ignorable = (Ignorable) o;
            return Objects.equals(globPattern, ignorable.globPattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(globPattern);
        }
    }
}
