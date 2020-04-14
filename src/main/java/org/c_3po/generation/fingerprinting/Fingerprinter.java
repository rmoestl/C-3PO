package org.c_3po.generation.fingerprinting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

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

    // TODO: Extract to helper and add tests that verify that
    //  the result is same a running `sha1sum` in the shell.
    private static byte[] computeSha1Hash(Path file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (var is = Files.newInputStream(file)) {
            // TODO: Consider increasing buffer size according to https://stackoverflow.com/a/237495/1029261.
            byte[] buffer = new byte[1024];

            int numBytesRead = 0;
            while (numBytesRead != -1) {
                numBytesRead = is.read(buffer);
                if (numBytesRead > 0) {

                    // It's crucial to not just pass `buffer` because then `.update`
                    // would add all of `buffer` even though `.read` might has read less bytes
                    // than buffer's length.
                    md.update(buffer, 0, numBytesRead);
                }
            }

            return md.digest();
        }
    }

    // TODO: Extract to helper class
    /**
     * Source: https://www.baeldung.com/java-byte-arrays-hex-strings
     */
    private static String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte b : byteArray) {
            hexStringBuffer.append(byteToHex(b));
        }
        return hexStringBuffer.toString();
    }

    // TODO: Extract to helper class
    /**
     * Source: https://www.baeldung.com/java-byte-arrays-hex-strings
     */
    private static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }
}
