package org.c_3po.generation.meta;

import org.c_3po.generation.GenerationException;
import org.c_3po.generation.IFileClassifier;
import org.c_3po.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

class MetadataExtractor {
    private final IFileClassifier fileClassifier;

    private MetadataExtractor(IFileClassifier fileClassifier) {
        this.fileClassifier = fileClassifier;
    }

    static MetadataExtractor getInstance(IFileClassifier fileClassifier) {
        return new MetadataExtractor(fileClassifier);
    }

    Metadata extract(Path file) throws GenerationException {
        if (fileClassifier.isHTML(file)) {
            return HTMLExtractor.extract(file);
        } else if (fileClassifier.isMarkdown(file)) {
            return MarkdownExtractor.extract(file);
        } else {
            throw new GenerationException(
                    "File '%s' has unknown file extension, can't extract meta data".formatted(file));
        }
    }

    private static String convertFilenameToTitle(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return StringUtils.capitalize(fileName
                .replaceAll("\\.(?:md|html)$", "")
                .replaceAll("[-_]", " "));
    }

    private static LocalDate getLastModifiedDateOfFile(Path filePath) throws IOException {
        FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
        return LocalDate.ofInstant(lastModifiedTime.toInstant(), ZoneId.systemDefault());
    }

    private static class HTMLExtractor {
        static Metadata extract(Path file) throws GenerationException {
            try {
                Document doc = Jsoup.parse(file.toFile());

                Element h1Elem = doc.select("h1").first();
                Element titleElem = doc.select("title").first();
                Element metaPublishTimeElem = doc.select("meta[property='article:published_time']").first();

                String title = h1Elem != null
                        ? h1Elem.text()
                        : titleElem != null
                            ? titleElem.text()
                            : convertFilenameToTitle(file);

                LocalDate publishDate = getLastModifiedDateOfFile(file);
                if (metaPublishTimeElem != null) {
                    try {
                        var content = metaPublishTimeElem.attr("content");
                        publishDate = LocalDate.parse(content, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    } catch (DateTimeParseException ex) {
                        // Ignore
                    }
                }

                return new Metadata(title, publishDate);
            } catch (IOException e) {
                throw new GenerationException("Failed extracting meta data from HTML file '%s'".formatted(file));
            }
        }
    }

    private static class MarkdownExtractor {
        private static final Pattern META_TITLE_PATTERN = Pattern.compile("^\\$meta-title: (.*)$");
        private static final Pattern META_PUBLISH_DATE_PATTERN =
                Pattern.compile("^\\$meta-publishDate: (\\d{4}-\\d{2}-\\d{2})\\s*$");
        private static final Pattern H1_PATTERN = Pattern.compile("^# (.*)$");

        static Metadata extract(Path file) throws GenerationException {
            try (var reader = Files.newBufferedReader(file)) {

                // Metadata variables
                String h1 = null;
                String metaTitle = null;
                LocalDate metaPublishDate = null;

                String line;
                while ((line = reader.readLine()) != null) {
                    var h1NotYetFound = h1 == null;
                    var metaTitleNotYetFound = metaTitle == null;

                    if (h1NotYetFound && metaTitleNotYetFound) {
                        metaTitle = tryExtractMetaTitle(line);
                    }

                    var metaPublishDateNotYetFound = metaPublishDate == null;
                    if (metaPublishDateNotYetFound) {
                        metaPublishDate = tryExtractMetaPublishDate(line);
                    }

                    if (h1NotYetFound) {
                        h1 = tryExtractH1(line);
                    }

                    // We found all metadata from their primary sources.
                    // Let's skip parsing any other lines.
                    if (h1 != null && metaPublishDate != null) {
                        break;
                    }
                }

                var title = h1 != null
                        ? h1
                        : metaTitle != null
                            ? metaTitle
                            : convertFilenameToTitle(file);
                var publishDate = metaPublishDate != null
                        ? metaPublishDate
                        : getLastModifiedDateOfFile(file);
                return new Metadata(title, publishDate);
            } catch (IOException e) {
                throw new GenerationException("Failed extracting meta data from Markdown file '%s'".formatted(file));
            }
        }

        private static String tryExtractMetaTitle(String line) {
            var matcher = META_TITLE_PATTERN.matcher(line);
            return matcher.find() ? matcher.group(1) : null;
        }

        private static LocalDate tryExtractMetaPublishDate(String line) {
            var matcher = META_PUBLISH_DATE_PATTERN.matcher(line);
            if (matcher.find()) {
                String publishDateVal = matcher.group(1);
                try {
                    return LocalDate.parse(publishDateVal);
                } catch (DateTimeParseException ex) {
                    // Ignore, cause no other way to detect a malformed date string.
                }
            }

            return null;
        }

        private static String tryExtractH1(String line) {
            var matcher = H1_PATTERN.matcher(line);
            return matcher.find() ? matcher.group(1) : null;
        }
    }
}
