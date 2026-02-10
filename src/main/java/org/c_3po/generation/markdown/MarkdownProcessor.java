package org.c_3po.generation.markdown;

import org.commonmark.Extension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Responsible for processing markdown files.
 */
public class MarkdownProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(MarkdownProcessor.class);

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    private MarkdownProcessor() {
        List<Extension> extensions = Collections.singletonList(MetaTagsParserExtension.getInstance());
        this.parser = Parser.builder().extensions(extensions).build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    public static MarkdownProcessor getInstance() {
        return new MarkdownProcessor();
    }

    public Result process(Path markdownFile) throws IOException {
        Objects.requireNonNull(markdownFile);
        if (Files.exists(markdownFile)) {
            LOG.debug("Processing markdown file '{}'", markdownFile);

            // Parse markdown
            Node document = parseDocument(markdownFile);

            // Produce HTML content
            String output = htmlRenderer.render(document);

            // Process meta tags
            Head head = extractMetadata(document);

            return new Result(head, output);
        } else {
            throw new FileNotFoundException("File '" + markdownFile.toAbsolutePath() + "' not found.");
        }
    }

    private Node parseDocument(Path file) throws IOException {
        return parser.parse(Files.readString(file));
    }

    private Head extractMetadata(Node document) {
        MetaTagsVisitor metaTagsVisitor = new MetaTagsVisitor();
        document.accept(metaTagsVisitor);
        return metaTagsVisitor.getResult();
    }

    public static class Result {
        private final Head headResult;
        private final String contentResult;

        public Result(Head headResult, String contentResult) {
            this.headResult = headResult;
            this.contentResult = contentResult;
        }

        public Head getHeadResult() {
            return headResult;
        }

        public String getContentResult() {
            return contentResult;
        }
    }

    public static class Head {
        private final Map<String, String> metaTags = new HashMap<>();

        private String title = "";

        public void setTitle(String title) {
            this.title = title;
        }

        void addMetaTag(String name, String content) {
            metaTags.put(name, content);
        }

        public String getTitle() {
            return title;
        }

        public Map<String, String> getMetaTags() {
            return new HashMap<>(metaTags);
        }
    }

    private static class MetaTagsVisitor extends AbstractVisitor {
        private static final String TITLE = "title";

        private final Head head = new Head();

        @Override
        public void visit(CustomBlock customBlock) {
            if (customBlock instanceof MetaTag) {
                MetaTag metaTag = (MetaTag) customBlock;
                if (TITLE.equals(metaTag.getName())) {
                    head.setTitle(metaTag.getContent());
                } else {
                    head.addMetaTag(metaTag.getName(), metaTag.getContent());
                }
            }
        }

        Head getResult() {
            return head;
        }
    }
}
