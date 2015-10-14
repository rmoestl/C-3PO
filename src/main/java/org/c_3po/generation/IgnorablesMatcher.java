package org.c_3po.generation;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helps to recognize ignorable files and directories.
 */
class IgnorablesMatcher {
    private final Path basePath;
    private final List<PathMatcher> pathMatchers;
    private List<String> globPatterns;

    private IgnorablesMatcher(Path basePath, List<String> globPatterns) {
        this.basePath = Objects.requireNonNull(basePath);
        this.globPatterns = new ArrayList<>(Objects.requireNonNull(globPatterns));
        this.pathMatchers = globPatterns.stream()
                .map(globPattern -> FileSystems.getDefault().getPathMatcher("glob:" + globPattern))
                .collect(Collectors.toList());
    }

    static IgnorablesMatcher from(Path basePath, List<String> globPatterns) {
        return new IgnorablesMatcher(basePath, globPatterns);
    }

    boolean matches(Path path) {
        Path relativePath = path.isAbsolute() ? basePath.relativize(path) : path.normalize();

        return pathMatchers.stream().anyMatch(matcher -> matcher.matches(relativePath));
    }

    public Path getBasePath() {
        return basePath;
    }

    public List<String> getGlobPatterns() {
        return globPatterns;
    }

    @Override
    public String toString() {
        return "IgnorablesMatcher{" +
                "basePath=" + basePath +
                ", pathMatchers=" + pathMatchers +
                '}';
    }
}
