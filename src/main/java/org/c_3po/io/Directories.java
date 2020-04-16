package org.c_3po.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Directories {

    /**
     * Copies a directory recursively.
     */
    public static void copyDir(Path sourceDirectory, Path targetDirectory) throws IOException {
        if (Files.exists(sourceDirectory)) {
            validateDirectory(sourceDirectory);
            validateDirectory(targetDirectory);

            if (!Files.exists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }

            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sourceDirectory)) {
                for (Path entry : directoryStream) {
                    if (Files.isDirectory(entry)) {
                        copyDir(entry, targetDirectory.resolve(entry.getFileName()));
                    } else {
                        Files.copy(entry, targetDirectory.resolve(entry.getFileName()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static void validateDirectory(Path directory) throws IllegalArgumentException {
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException(directory.toAbsolutePath() + " is not a directory");
        }
        if (Files.exists(directory) && !Files.isReadable(directory)) {
            throw new IllegalArgumentException("Read access denied for " + directory.toAbsolutePath());
        }
        if (Files.exists(directory) && !Files.isWritable(directory)) {
            throw new IllegalArgumentException("Write access denied for " + directory.toAbsolutePath());
        }
    }
}
