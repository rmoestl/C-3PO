package org.c_3po.generation.meta;

import org.c_3po.generation.GenerationException;
import org.c_3po.generation.IFileClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class PageTreeBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PageTreeBuilder.class);

    private final IFileClassifier fileClassifier;
    private final MetadataExtractor metaDataExtractor;

    private PageTreeBuilder(IFileClassifier fileClassifier) {
        this.fileClassifier = fileClassifier;
        this.metaDataExtractor = MetadataExtractor.getInstance(fileClassifier);
    }

    public static PageTreeBuilder getInstance(IFileClassifier fileClassifier) {
        return new PageTreeBuilder(fileClassifier);
    }

    public PageTree obtainFrom(Path srcDir) {
        LOG.info("Obtaining page tree from '{}'", srcDir);

        try {
            var pageTree = new PageTree();

            Files.walkFileTree(srcDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var isNotIgnorable = !fileClassifier.isCompleteIgnorable(file)
                            && !fileClassifier.isResultIgnorable(file);
                    var willBePage = Files.isRegularFile(file)
                            && (fileClassifier.isHTML(file) || fileClassifier.isMarkdown(file));

                    if (isNotIgnorable && willBePage) {

                        // Note: Fully aware this no longer works when page urls no longer end with ".html".
                        try {
                            var urlPath = srcDir.relativize(file).toString().replaceFirst("\\.md$", ".html");
                            var metaData = metaDataExtractor.extract(file);
                            var page = new Page(Paths.get("/", urlPath), metaData.title(), metaData.publishDate());
                            pageTree.add(page);
                        } catch (GenerationException e) {
                            throw new IOException(e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return fileClassifier.isCompleteIgnorable(dir) || fileClassifier.isResultIgnorable(dir)
                            ? FileVisitResult.SKIP_SUBTREE
                            : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    // Note: This is likely when a temp file was existing (e.g. from editing with vim)
                    // when the file walk began but no longer exists. We simply continue. The default
                    // impl. would through an IOException.
                    // See https://stackoverflow.com/questions/72851557/why-java-files-walkfiletree-throw-a-nosuchfileexception
                    return FileVisitResult.CONTINUE;
                }
            });

            return pageTree;
        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain page tree. Reason: " + e.getMessage(), e);
        }
    }
}
