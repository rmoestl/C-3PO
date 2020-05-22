package org.c_3po.generation.assets;

import org.c_3po.io.FileFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.c_3po.util.ChecksumCalculator.computeSha1Hash;
import static org.c_3po.util.ChecksumCalculator.encodeHexString;

public class Fingerprinter {
    private static final Logger LOG = LoggerFactory.getLogger(Fingerprinter.class);

    public static Map<String, String> fingerprintStylesheets(Path dir, Path rootDestDir)
            throws IOException, NoSuchAlgorithmException {
        return fingerprintAssets(dir, rootDestDir, "css");
    }

    public static Map<String, String> fingerprintJsFiles(Path dir, Path rootDestDir)
            throws IOException, NoSuchAlgorithmException {
        return fingerprintAssets(dir, rootDestDir, "js");
    }

    public static Map<String, String> fingerprintImageFiles(Path dir, Path rootDestDir)
            throws IOException, NoSuchAlgorithmException {
        return fingerprintAssets(dir, rootDestDir, "png", "jpg", "jpeg", "svg", "gif", "webp");
    }

    private static Map<String, String> fingerprintAssets(Path dir, Path rootDestDir, String... fileExtensions)
            throws IOException, NoSuchAlgorithmException {
        final var extensionsRegex = "\\.(" + String.join("|", fileExtensions) + ")$";
        final var fingerprintedFileRegex = "\\.[0123456789abcdef]{40}" + extensionsRegex;
        final var filePattern = Pattern.compile(extensionsRegex, Pattern.CASE_INSENSITIVE);
        final var fingerprintedFilePattern = Pattern.compile(fingerprintedFileRegex, Pattern.CASE_INSENSITIVE);
        final var substitutes = new HashMap<String, String>();

        // If no valid directory, return empty map
        if (!Files.isDirectory(dir)) {
            return substitutes;
        }

        DirectoryStream.Filter<Path> assetFileFilter =
                entry -> {
                    String fileName = entry.toFile().getName();
                    return Files.isRegularFile(entry)
                            && filePattern.matcher(fileName).find()
                            && !fingerprintedFilePattern.matcher(fileName).find();
                };

        try (DirectoryStream<Path> assetFiles = Files.newDirectoryStream(dir, assetFileFilter)) {
            for (Path assetFile : assetFiles) {
                LOG.info(String.format("Fingerprinting asset file '%s'", assetFile));

                // Compute hash
                String sha1 = encodeHexString(computeSha1Hash(assetFile));

                // Create file
                String fileName = assetFile.getFileName().toString();
                String fileNameExt = fileName.substring(fileName.lastIndexOf(".") + 1);
                String fingerprintedFileName = fileName.replaceFirst(extensionsRegex, "." + sha1 + "." + fileNameExt);
                Path fingerprintedFile = dir.resolve(fingerprintedFileName);
                if (!Files.exists(fingerprintedFile)) {
                    Files.copy(assetFile, fingerprintedFile);
                }

                // Add substitution
                Path dirAsUrlPath = rootDestDir.toAbsolutePath().relativize(dir.toAbsolutePath());

                // Note: Leading slash makes it comparable to "implicit schema and domain absolute URLs"
                substitutes.put("/" + dirAsUrlPath.resolve(fileName).toString(),
                        "/" + dirAsUrlPath.resolve(fingerprintedFileName).toString());

                // Purge any outdated fingerprinted versions of this file
                purgeOutdatedFingerprintedVersions(dir, fileName, fingerprintedFileName);
            }
        }

        // Recurse into sub directories
        try (DirectoryStream<Path> subDirs = FileFilters.subDirStream(dir)) {
            for (Path subDir : subDirs) {
                substitutes.putAll(fingerprintAssets(subDir, rootDestDir, fileExtensions));
            }
        }

        return substitutes;
    }

    private static void purgeOutdatedFingerprintedVersions(Path dir, String fileName,
                                                                          String fingerprintedFileName)
            throws IOException {
        var name = fileName.substring(0, fileName.lastIndexOf("."));
        var ext = fileName.substring(fileName.lastIndexOf(".") + 1);

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
