package org.c_3po.generation.crawl;

import org.c_3po.generation.GenerationException;
import org.c_3po.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class that generates a simple robots.txt file.
 */
public class RobotsGenerator {
    public static final String ROBOTS_TXT_FILE_NAME = "robots.txt";

    // Make it non-instantiable and prohibit subclassing.
    private RobotsGenerator() {
        throw new AssertionError();
    }

    public static void generate(Path parentDirectoryPath, String sitemapUrl) throws GenerationException {
        try {
            Objects.requireNonNull(parentDirectoryPath, "parentDirectoryPath must not be null");
            Files.createDirectories(parentDirectoryPath);
            Path robotsFilePath = parentDirectoryPath.resolve(ROBOTS_TXT_FILE_NAME);
            Files.write(robotsFilePath, createContents(sitemapUrl));
        } catch (IOException e) {
            throw new GenerationException(String.format("Failed to write '%s' file to '%s'", ROBOTS_TXT_FILE_NAME, parentDirectoryPath), e);
        }
    }

    private static List<String> createContents(String sitemapUrl) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("# www.robotstxt.org/\n" +
                "\n" +
                "# Allow crawling of all content\n" +
                "User-agent: *\n" +
                "Disallow:");
        if (!StringUtils.isBlank(sitemapUrl)) {
            lines.add(String.format("\nSitemap: %s", sitemapUrl));
        }
        return lines;
    }
}
