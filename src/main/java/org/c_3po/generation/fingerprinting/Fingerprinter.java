package org.c_3po.generation.fingerprinting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.c_3po.util.ChecksumCalculator.computeSha1Hash;
import static org.c_3po.util.ChecksumCalculator.encodeHexString;

public class Fingerprinter {
    private static final Logger LOG = LoggerFactory.getLogger(Fingerprinter.class);

    // TODO: Tests
    //  - CSS in root
    //  - CSS in subdir
    //  - Already fingerprinted files in the output do not get fingerprinted again
    //  - Old fingerprinted files in output get deleted if the fingerprint has changed
    //  - A reference to an old fingerprinted version is replaced by a new one
    public static Map<String, String> fingerprintStylesheets(Path dir, Path rootDestDir)
            throws IOException, NoSuchAlgorithmException {
        var substitutes = new HashMap<String, String>();

        // If no valid directory, return empty map
        if (!Files.isDirectory(dir)) {
            return substitutes;
        }

        DirectoryStream.Filter<Path> cssFilter =
                entry -> {
                    String fileName = entry.toFile().getName();
                    return Files.isRegularFile(entry)
                            && fileName.endsWith(".css")
                            && !fileName.matches("^.*\\.[0123456789abcdef]{40}\\.css$");
                };

        try (DirectoryStream<Path> cssFiles = Files.newDirectoryStream(dir, cssFilter)) {
            for (Path cssFile : cssFiles) {
                LOG.info(String.format("Fingerprinting stylesheet file '%s'", cssFile));

                // Compute hash
                String sha1 = encodeHexString(computeSha1Hash(cssFile));

                // Create file
                String fileName = cssFile.getFileName().toString();
                String fingerprintedFileName = fileName.replaceFirst(".css$", "." + sha1 + ".css");
                Path fingerprintedFile = dir.resolve(fingerprintedFileName);
                if (!Files.exists(fingerprintedFile)) {
                    Files.copy(cssFile, fingerprintedFile);
                }

                // Add substitution
                // TODO: See if that works for subfolders in /css as well
                Path dirAsUrlPath = rootDestDir.toAbsolutePath().relativize(dir.toAbsolutePath());

                // Note: Leading slash makes it comparable to "implicit schema and domain absolute URLs"
                substitutes.put("/" + dirAsUrlPath.resolve(fileName).toString(),
                        dirAsUrlPath.resolve(fingerprintedFileName).toString());

                // Purge any outdated fingerprinted versions of this file
                // TODO: Add substitutes from here as well
                purgeOutdatedFingerprintedVersions(dir, fileName, fingerprintedFileName);
            }
        }

        // Recurse into sub directories
        try (DirectoryStream<Path> subDirs = Files.newDirectoryStream(dir, entry -> Files.isDirectory(entry))) {
            for (Path subDir : subDirs) {
                substitutes.putAll(fingerprintStylesheets(subDir, rootDestDir));
            }
        }

        return substitutes;
    }

    private static void purgeOutdatedFingerprintedVersions(Path dir, String fileName, String fingerprintedFileName)
            throws IOException {
        String name = fileName.substring(0, fileName.lastIndexOf("."));
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);

        DirectoryStream.Filter<Path> outdatedFilter =
                entry -> {
                    String entryFileName = entry.toFile().getName();
                    return Files.isRegularFile(entry)
                            && !entryFileName.equals(fingerprintedFileName)
                            && entryFileName.matches(String.format("^%s\\.[0123456789abcdef]{40}\\.%s$", name, ext));
                };

        try (DirectoryStream<Path> outdatedFiles = Files.newDirectoryStream(dir, outdatedFilter)) {
            for (Path outdatedFile : outdatedFiles) {
                Files.delete(outdatedFile);
            }
        }
    }
}
