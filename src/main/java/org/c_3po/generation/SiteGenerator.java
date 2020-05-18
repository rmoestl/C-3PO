package org.c_3po.generation;

import io.bit3.jsass.CompilationException;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingStrategy;
import org.c_3po.cmd.CmdArguments;
import org.c_3po.generation.assets.AssetReferences;
import org.c_3po.generation.assets.Fingerprinter;
import org.c_3po.generation.crawl.RobotsGenerator;
import org.c_3po.generation.crawl.SiteStructure;
import org.c_3po.generation.crawl.SitemapGenerator;
import org.c_3po.generation.markdown.MarkdownProcessor;
import org.c_3po.generation.sass.SassProcessor;
import org.c_3po.io.FileFilters;
import org.c_3po.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Node;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Main class responsible for site generation.
 */
public class SiteGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SiteGenerator.class);
    private static final String C_3PO_IGNORE_FILE_NAME = ".c3poignore";
    private static final String C_3PO_SETTINGS_FILE_NAME = ".c3posettings";
    private static final String CONVENTIONAL_MARKDOWN_TEMPLATE_NAME = "md-template.html";

    private final Path sourceDirectoryPath;
    private final Path destinationDirectoryPath;
    private final boolean shouldFingerprintAssets;
    private final Properties settings;

    private final DirectoryStream.Filter<Path> sourceHtmlFilter =
            entry -> !isCompleteIgnorable(entry)
                    && !isResultIgnorable(entry)
                    && FileFilters.htmlFilter.accept(entry);
    private final DirectoryStream.Filter<Path> markdownFilter =
            entry -> Files.isRegularFile(entry) && !isCompleteIgnorable(entry) && !isResultIgnorable(entry) && entry.toFile().getName().endsWith(".md");
    private final DirectoryStream.Filter<Path> markdownTemplateFilter =
            entry -> Files.isRegularFile(entry) && !isCompleteIgnorable(entry) && entry.toFile().getName().equals(CONVENTIONAL_MARKDOWN_TEMPLATE_NAME);

    private final DirectoryStream.Filter<Path> sassFilter =
            entry -> {
                boolean isSassFile = entry.toFile().getName().endsWith(".sass")
                        || entry.toFile().getName().endsWith(".scss");
                return Files.isRegularFile(entry)
                        && !isCompleteIgnorable(entry) && !isResultIgnorable(entry)
                        && isSassFile;
            };

    private final DirectoryStream.Filter<Path> staticFileFilter =
            entry -> Files.isRegularFile(entry) && !isCompleteIgnorable(entry) && !isResultIgnorable(entry) && !sourceHtmlFilter.accept(entry)
                    && !markdownFilter.accept(entry) && !sassFilter.accept(entry);
    private final TemplateEngine templateEngine;
    private final MarkdownProcessor markdownProcessor;
    private final SassProcessor sassProcessor;

    private IgnorablesMatcher completeIgnorablesMatcher;
    private IgnorablesMatcher resultIgnorablesMatcher;

    private SiteGenerator(Path sourceDirectoryPath, Path destinationDirectoryPath, boolean fingerprintAssets,
                          List<String> completeIgnorables, List<String> resultIgnorables,
                          Properties settings) {
        this.sourceDirectoryPath = sourceDirectoryPath;
        this.destinationDirectoryPath = destinationDirectoryPath;
        this.shouldFingerprintAssets = fingerprintAssets;
        this.settings = settings;
        this.templateEngine = setupTemplateEngine(sourceDirectoryPath);
        this.markdownProcessor = MarkdownProcessor.getInstance();
        this.sassProcessor = SassProcessor.getInstance();
        this.completeIgnorablesMatcher = IgnorablesMatcher.from(sourceDirectoryPath, completeIgnorables);
        this.resultIgnorablesMatcher = IgnorablesMatcher.from(sourceDirectoryPath, resultIgnorables);
    }

    /**
     * Factory method that creates a SiteGenerator from command line arguments.
     */
    public static SiteGenerator fromCmdArguments(CmdArguments cmdArguments) {
        Objects.requireNonNull(cmdArguments);
        Path sourceDirectoryPath = Paths.get(cmdArguments.getSourceDirectory());

        ensureValidSourceDirectory(sourceDirectoryPath);

        // Read in settings
        Path settingsFilePath = sourceDirectoryPath.resolve(C_3PO_SETTINGS_FILE_NAME);
        Properties settings = null;
        try {
            settings = readSettings(settingsFilePath);
        } catch (IOException e) {
            LOG.warn("Failed to load settings from file '{}'", settingsFilePath);
        }

        // Construct instance
        return new SiteGenerator(sourceDirectoryPath,
                Paths.get(cmdArguments.getDestinationDirectory()),
                cmdArguments.shouldFingerprintAssets(),
                getCompleteIgnorables(sourceDirectoryPath),
                Ignorables.readResultIgnorables(sourceDirectoryPath.resolve(C_3PO_IGNORE_FILE_NAME)), settings);
    }

    private static void ensureValidSourceDirectory(Path sourceDirectoryPath) {
        if (!Files.exists(sourceDirectoryPath)) {
            throw new IllegalArgumentException(
                    "Source directory '" + sourceDirectoryPath + "' does not exist.");
        }
        if (!Files.isDirectory(sourceDirectoryPath)) {
            throw new IllegalArgumentException(
                    "Source directory '" + sourceDirectoryPath + "' is not a directory.");
        }
    }

    private static Properties readSettings(Path settingsFilePath) throws IOException {
        Properties properties = new Properties();
        if (Files.exists(settingsFilePath)) {
            properties.load(Files.newInputStream(settingsFilePath));
        }
        return properties;
    }

    /**
     * Does a one time site generation.
     * @throws IOException
     */
    public void generate() throws IOException, GenerationException {
        buildWebsite();

        // TODO: Clarify why this should not be supported in auto-build mode or simply
        //  make it work in auto-build mode.
        buildCrawlFiles();

        // TODO Check if there are any files in destination directory that are to be ignored
        //  (e.g. because ignore file has changed since last generation)
        //  Update 2020-03-02: Not sure if `generate` is the right place to do so.
    }

    /**
     * Does a site generation when a source file has been added, changed or deleted
     * @throws IOException
     */
    public void generateOnFileChange() throws IOException, GenerationException {
        buildWebsite();

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
                    updateIgnorables(changedPath);
                } else {
                    Path parent = watchKeyMap.get(key);
                    changedPath = parent.resolve(changedPath);

                    if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                        if (sourceHtmlFilter.accept(changedPath) || sassFilter.accept(changedPath)) {
                            buildWebsite();
                        } else if (staticFileFilter.accept(changedPath) ||
                                markdownFilter.accept(changedPath) ||
                                markdownTemplateFilter.accept(changedPath)) {
                            Path parentDir = sourceDirectoryPath.relativize((Path) key.watchable());

                            // Changed static assets and markdown articles don't require a full rebuild
                            // because their contents isn't copied over into another file.
                            buildPartOfWebsite(parentDir);
                        } else if (Files.isDirectory(changedPath) && !isCompleteIgnorable(changedPath)) {
                            if (kind == ENTRY_CREATE) {
                                watchKeyMap.put(registerWatchService(watchService, changedPath), changedPath);
                                LOG.debug("Registered autoBuild watcher for '{}'", changedPath);
                            }
                            buildWebsite();
                        } else {
                            LOG.warn("No particular action executed for '{}' that triggered a change with kind '{}'",
                                    event.context(), event.kind());
                        }
                    } else if (kind == ENTRY_DELETE) {
                        if (!isCompleteIgnorable(changedPath) && !isResultIgnorable(changedPath)) {
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
                if (!isCompleteIgnorable(Objects.requireNonNull(dir).normalize())) {
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

    private void buildWebsite() throws IOException, GenerationException {
        LOG.debug("Building entire website");

        buildPagesAndAssets(sourceDirectoryPath, destinationDirectoryPath);

        // TODO: Somehow use purge-css as well if the respective flag is set
        fingerprintAssetsIfEnabled();
    }

    private void buildPartOfWebsite(Path srcSubDir) throws IOException, GenerationException {
        LOG.debug("Building part of website contained in '{}'", srcSubDir);

        buildPagesAndAssets(srcSubDir, destinationDirectoryPath.resolve(srcSubDir));

        // TODO: Somehow use purge-css as well if the respective flag is set
        fingerprintAssetsIfEnabled();
    }

    private void buildPagesAndAssets(Path sourceDir, Path targetDir) throws IOException {
        LOG.debug("Building pages and assets contained in '{}'", sourceDir);

        // Clear Thymeleaf's template cache
        templateEngine.clearTemplateCache();

        // Ensure targetDir exists
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Look for HTML files to generate
        try (DirectoryStream<Path> htmlFilesStream = Files.newDirectoryStream(sourceDir, sourceHtmlFilter)) {
            for (Path htmlFile : htmlFilesStream) {
                LOG.trace("Generate '{}'", htmlFile);

                // Generate
                try {
                    List<String> lines = Collections.singletonList(
                            templateEngine.process(htmlFile.toString().replace(".html", ""), getBaseTemplateContext()));
                    // Write to file
                    Path destinationPath = targetDir.resolve(htmlFile.getFileName());
                    try {
                        Files.write(destinationPath, lines, Charset.forName("UTF-8"), CREATE, WRITE, TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        LOG.error("Failed to write generated document to {}", destinationPath, e);
                    }
                } catch (RuntimeException ex) {
                    LOG.warn("Thymeleaf failed to process '{}'. Reason: '{}'", htmlFile, ex.getMessage());
                }
            }
        }

        // Look for Markdown files to generate
        try (DirectoryStream<Path> markdownFilesStream = Files.newDirectoryStream(sourceDir, markdownFilter)) {
            Iterator<Path> iterator = markdownFilesStream.iterator();
            if (iterator.hasNext()) {
                Path markdownTemplatePath = sourceDir.resolve(CONVENTIONAL_MARKDOWN_TEMPLATE_NAME);
                if (Files.exists(markdownTemplatePath)) {
                    String markdownTemplateName = markdownTemplatePath.toString().replace(".html", "");
                    while (iterator.hasNext()) {
                        final Path markdownFile = iterator.next();
                        try {
                            // Process markdown
                            MarkdownProcessor.Result mdResult = markdownProcessor.process(markdownFile);

                            // Integrate into Thymeleaf template
                            Context context = getBaseTemplateContext();
                            context.setVariable("markdownContent", mdResult.getContentResult());
                            context.setVariable("markdownHead", mdResult.getHeadResult());
                            context.setVariable("markdownFileName", markdownFile.toString());
                            String result = templateEngine.process(markdownTemplateName, context);

                            // Write result to file
                            Path destinationPath = targetDir.resolve(markdownFile.getFileName().toString().replace(".md", ".html"));
                            Files.write(destinationPath, Collections.singletonList(result), Charset.forName("UTF-8"), CREATE,
                                    WRITE, TRUNCATE_EXISTING);
                        } catch (IOException e) {
                            LOG.error("Failed to generate document from markdown '{}': [{}]", markdownFile, e.getMessage());
                        }
                    }
                } else {
                    LOG.warn("Not processing markdown files in '{}' because expected template file '{}' is missing",
                            sourceDir, markdownTemplatePath + ".html");
                }
            }
        }

        // Look for SASS files to generate
        try (DirectoryStream<Path> sassFilesStream = Files.newDirectoryStream(sourceDir, sassFilter)) {
            for (Path sassFile : sassFilesStream) {
                try {
                    boolean isNotSassPartial = !sassFile.toFile().getName().startsWith("_");

                    if (isNotSassPartial) {
                        String result = sassProcessor.process(sassFile);
                        Path destinationPath = targetDir.resolve(sassFile.getFileName().toString()
                                .replace(".sass", ".css")
                                .replace(".scss", ".css"));
                        Files.write(destinationPath, Collections.singletonList(result), Charset.forName("UTF-8"), CREATE,
                                WRITE, TRUNCATE_EXISTING);
                    }
                } catch (CompilationException e) {
                    LOG.error("Failed to process SASS file '{}'", sassFile, e);
                }
            }
        }

        // Look for static files to synchronize
        try (DirectoryStream<Path> staticFilesStream = Files.newDirectoryStream(sourceDir, staticFileFilter)) {
            for (Path staticFile : staticFilesStream) {
                Files.copy(staticFile, targetDir.resolve(staticFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Look for subdirectories to be processed
        try (DirectoryStream<Path> subDirStream =
                     Files.newDirectoryStream(sourceDir,
                             entry -> Files.isDirectory(entry) && !isCompleteIgnorable(entry.normalize())
                                     && !isResultIgnorable(entry.normalize()))) {
            for (Path subDir : subDirStream) {
                LOG.trace("I'm going to build pages in this subdirectory [{}]", subDir);
                buildPagesAndAssets(subDir, targetDir.resolve(subDir.getFileName()));
            }
        }
    }

    private Context getBaseTemplateContext() {
        Context context = new Context();
        context.setVariable("year", LocalDateTime.now().get(ChronoField.YEAR));
        return context;
    }

    /**
     * Builds the crawling-related files sitemap.xml and robots.txt.
     */
    private void buildCrawlFiles() {
        String sitemapFileName = "sitemap.xml";
        String baseUrl = settings.getProperty("baseUrl");

        boolean noSitemapFileInSourceDir = !Files.exists(sourceDirectoryPath.resolve(sitemapFileName));
        boolean baseSiteUrlIsSet = !StringUtils.isBlank(baseUrl);
        if (noSitemapFileInSourceDir && baseSiteUrlIsSet) {
            try {
                SiteStructure siteStructure = SiteStructure.getInstance(baseUrl);

                // Capture site structure
                IgnorablesMatcher sitemapIgnorablesMatcher = IgnorablesMatcher.from(destinationDirectoryPath,
                        Ignorables.readSitemapIgnorables(sourceDirectoryPath.resolve(C_3PO_IGNORE_FILE_NAME)));
                Files.walkFileTree(destinationDirectoryPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        boolean isHtmlFile = file.toString().endsWith(".html");
                        boolean isNotIgnorable = !sitemapIgnorablesMatcher.matches(file);
                        if (isHtmlFile && isNotIgnorable) {
                            siteStructure.add(destinationDirectoryPath.relativize(file));
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (sitemapIgnorablesMatcher.matches(dir)) {
                            LOG.debug("Ignoring directory '{}' for sitemap generation", dir.toAbsolutePath());
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                // Build the sitemap.xml and robots.txt
                try {
                    // sitemap.xml
                    LOG.info("Building a sitemap xml file");
                    Path sitemapFilePath = destinationDirectoryPath.resolve(sitemapFileName);
                    SitemapGenerator.generate(siteStructure, sitemapFilePath);

                    // robots.txt
                    // TODO: This check should be moved one level up
                    if (!Files.exists(sourceDirectoryPath.resolve(RobotsGenerator.ROBOTS_TXT_FILE_NAME))) {
                        try {
                            LOG.info("Building a robots.txt file");
                            RobotsGenerator.generate(destinationDirectoryPath, StringUtils.trimmedJoin("/", baseUrl, sitemapFileName));
                        } catch (GenerationException e) {
                            LOG.warn("Wasn't able to generate a '{}' file. Proceeding.",
                                    RobotsGenerator.ROBOTS_TXT_FILE_NAME,  e);
                        }
                    } else {
                        LOG.info("Found a robots.txt file in '{}'. Tip: ensure that the URL to the sitemap.xml " +
                                "file is included in robots.txt.", sourceDirectoryPath);
                    }
                } catch (GenerationException e) {
                    LOG.warn("Failed to generate sitemap xml file", e);
                }
            } catch (IOException e) {
                LOG.warn("Failed to generate sitemap file.", e);
            }
        }
    }

    private void fingerprintAssetsIfEnabled() throws IOException {
        if (this.shouldFingerprintAssets) {

            var assetSubstitutes = new HashMap<String, String>();
            try {
                var stylesheetDir = destinationDirectoryPath.resolve("css");
                var jsDir = destinationDirectoryPath.resolve("js");
                assetSubstitutes.putAll(Fingerprinter.fingerprintStylesheets(stylesheetDir, destinationDirectoryPath));
                assetSubstitutes.putAll(Fingerprinter.fingerprintJsFiles(jsDir, destinationDirectoryPath));
            } catch (NoSuchAlgorithmException e) {
                LOG.warn("Failed to fingerprint assets. Beware that your cache busting may not work.");
            }


            // Replace references
            LOG.info(assetSubstitutes.toString());
            AssetReferences.replaceAssetsReferences(destinationDirectoryPath, assetSubstitutes, settings);
        }
    }

    private TemplateEngine setupTemplateEngine(Path sourceDirectoryPath) {
        TemplateEngine templateEngine = new TemplateEngine();

        // Note: we need two FileTemplateResolvers
        // one that is able to deal with absolute path template names like 'D:/data/dev/blog/index'
        // and one that is able to resolve relative path template names like '_layouts/main-layout'
        templateEngine.addTemplateResolver(newTemplateResolver(sourceDirectoryPath.toAbsolutePath()));
        templateEngine.addTemplateResolver(newTemplateResolver());
        templateEngine.addDialect(new LayoutDialect(new EnhancedGroupingStrategy()));
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

    /**
     * Reads complete ignorables from ignore file and adds C-3PO standard files.
     */
    private static List<String> getCompleteIgnorables(Path baseDirectory) {
        List<String> ignorables = new ArrayList<>();

        // System standard ignorables
        ignorables.add(C_3PO_IGNORE_FILE_NAME);
        ignorables.add(C_3PO_SETTINGS_FILE_NAME);

        // User-specific ignorables
        List<String> ignorablesFromFile = Ignorables.readCompleteIgnorables(baseDirectory.resolve(C_3PO_IGNORE_FILE_NAME));
        ignorables.addAll(ignorablesFromFile);

        return ignorables;
    }

    private boolean isCompleteIgnorable(Path path) throws IOException {
        return completeIgnorablesMatcher.matches(path)
                || Files.exists(destinationDirectoryPath) && Files.exists(path) && Files.isSameFile(path, destinationDirectoryPath);
    }

    private boolean isResultIgnorable(Path path) throws IOException {
        return resultIgnorablesMatcher.matches(path)
                || Files.exists(destinationDirectoryPath) && Files.exists(path) && Files.isSameFile(path, destinationDirectoryPath);
    }

    private void updateIgnorables(Path ignorablesFile) {
        List<String> newCompleteIgnorables = getCompleteIgnorables(ignorablesFile);
        List<String> newResultIgnorables = Ignorables.readResultIgnorables(ignorablesFile);

        cleanOutputFromAddedIgnorables(newCompleteIgnorables, completeIgnorablesMatcher.getGlobPatterns());
        cleanOutputFromAddedIgnorables(newResultIgnorables, resultIgnorablesMatcher.getGlobPatterns());

        this.completeIgnorablesMatcher = IgnorablesMatcher.from(destinationDirectoryPath, newCompleteIgnorables);
        this.resultIgnorablesMatcher = IgnorablesMatcher.from(destinationDirectoryPath, newResultIgnorables);
    }

    private void cleanOutputFromAddedIgnorables(List<String> newIgnorables, List<String> presentIgnorables) {
        List<String> addedIgnorables = new ArrayList<>(newIgnorables);
        addedIgnorables.removeAll(presentIgnorables);
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

    /**
     * Enhancing / fixing Layout Dialect's GroupingStrategy which doesn't know about
     * icon elements in &lt;head&gt;.
     *
     * E.g. I had the problem of using &lt;link&gt; rel="icon"... in the layout
     * which is not known to GroupingStrategy. This somehow caused the very
     * important &lt;base&gt; element to be at the bottom of head which resulted in
     * CSS files etc. to not resolve correctly.
     */
    private static class EnhancedGroupingStrategy implements SortingStrategy {
        private final GroupingStrategy delegate;

        EnhancedGroupingStrategy() {
            this.delegate = new GroupingStrategy();
        }

        @Override
        public int findPositionForContent(List<Node> decoratorNodes, Node contentNode) {
            if (contentNode instanceof Element
                    && ((Element) contentNode).getNormalizedName().equals("base")) {
                return 0;
            } else {
                return this.delegate.findPositionForContent(decoratorNodes, contentNode);
            }
        }
    }
}
