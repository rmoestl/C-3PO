package org.c_3po.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Noninstantiable utility class responsible for reading C-3PO ignore file.
 */
class Ignorables {
    private static final Logger LOG = LoggerFactory.getLogger(Ignorables.class);

    // Make it non-instantiable and prohibit subclassing.
    private Ignorables() {
        throw new AssertionError();
    }

    /**
     * Reads the contents of an ignore file and returns a list of strings as glob patterns that represent
     * files and directories that should be ignored completely.
     * @param ignoreFile the path to the ignore file
     * @return a list of glob pattern strings (without 'glob:' prefix)
     */
    static List<String> readCompleteIgnorables(Path ignoreFile) {
        return read(ignoreFile, line -> !line.matches(".*\\s\\[[a-z, ]*\\]\\s*$"));
    }

    /**
     * Reads the contents of an ignore file and returns a list of strings as glob patterns that represent
     * files and directories that should be ignored only when generating a sitemap.
     * @param ignoreFile the path to the ignore file
     * @return a list of glob pattern strings (without 'glob:' prefix)
     */
    static List<String> readSitemapIgnorables(Path ignoreFile) {
        return read(ignoreFile, line -> line.matches(".*\\s\\[[a-z, ]*es[a-z, ]*\\]\\s*$"));
    }

    private static List<String> read(Path ignoreFile, Predicate<? super String> filter) {
        List<String> ignorablePaths = new ArrayList<>();
        Path ignoreFileFileName = ignoreFile.getFileName();

        if (Files.exists(ignoreFile)) {
            if (Files.isRegularFile(ignoreFile) && Files.isReadable(ignoreFile)) {
                try {
                    ignorablePaths = Files.readAllLines(ignoreFile)
                            .stream()
                            .filter(filter)
                            .map(line -> line.replaceAll("\\s\\[[a-z, ]*\\]\\s*$", ""))
                            .collect(Collectors.toList());
                    LOG.info("'{}' read ignorables file successfully", ignoreFileFileName);
                } catch (IOException e) {
                    LOG.error("Failed to read '{}' from '{}'. No files and directories will be ignored by C-3PO " +
                            "during processing", ignoreFileFileName, ignoreFile, e);
                }
            } else {
                LOG.info("Invalid '{}' file found. Make sure it's a regular file and readable", ignoreFile);
            }
        } else {
            LOG.info("No '{}' file detected in directory '{}'. " +
                            "In a '{}' file you can exclude files and folders from being processed by C-3PO",
                    ignoreFileFileName, ignoreFile.getParent(), ignoreFileFileName);
        }

        return ignorablePaths;
    }
}
