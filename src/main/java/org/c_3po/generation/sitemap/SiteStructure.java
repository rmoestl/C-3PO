package org.c_3po.generation.sitemap;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the structure of a generated website.
 */
public class SiteStructure {
    public static final String URL_PATH_DELIMITER = "/";
    private final String baseUrl;
    private List<Path> paths = new ArrayList<>();

    private SiteStructure(String baseUrl) {
        this.baseUrl = withTrailingSlash(baseUrl);
    }

    public static SiteStructure getInstance(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        return new SiteStructure(baseUrl);
    }

    /**
     * Adds a new page to the site structure defined by the passed path
     * @param path must be a relative path
     * @throws IllegalArgumentException if passed path is null or absolute
     */
    public void add(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Path must not be an absolute path.");
        }
        paths.add(path);
    }

    public List<String> toUrls() {
        return paths.stream().map(pagePath -> baseUrl + toUrlPart(pagePath)).collect(Collectors.toList());
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private String withTrailingSlash(String s) {
        return s.endsWith(URL_PATH_DELIMITER) ? s : s + URL_PATH_DELIMITER;
    }

    private String toUrlPart(Path pagePath) {
        StringJoiner joiner = new StringJoiner(URL_PATH_DELIMITER);
        pagePath.forEach(pathElement -> joiner.add(pathElement.toString()));
        return joiner.toString();
    }
}
