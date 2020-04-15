package org.c_3po.generation.assets;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A Java class because Groovy version at the time of writing haven't supported
 * Java Lambdas and try-with-resources.
 */
class FingerprinterSpecHelper {
    static void cleanupFingerprintedFiles(Path dir) throws IOException {
        DirectoryStream.Filter<Path> fingerprintedCssFilter = entry -> {
            String fileName = entry.toFile().getName();
            return Files.isRegularFile(entry)
                    && fileName.endsWith(".css")
                    && fileName.matches("^.*\\.[0123456789abcdef]{40}\\.css$");
        };

        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, fingerprintedCssFilter)) {
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        }

        try (DirectoryStream<Path> subDirs = Files.newDirectoryStream(dir, entry -> Files.isDirectory(entry))) {
            for (Path subDir : subDirs) {
                cleanupFingerprintedFiles(subDir);
            }
        }
    }
}
