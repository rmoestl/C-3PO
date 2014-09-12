package org.c_3po.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Responsible for synchronizing the contents of directories. E.g. useful
 * for keeping static resources directories such as "img/" in sync with the
 * output folder.
 */
public class DirectorySynchronizer {
    public void sync(String sourceDirectoryPath, String targetPath) throws IOException {
        File sourceDirectory = new File(sourceDirectoryPath);
        File targetDirectory = new File(targetPath);
        validateDirectory(sourceDirectory);
        validateDirectory(targetDirectory);

        if (!targetDirectory.exists()) {
            Files.createDirectory(targetDirectory.toPath());
        }
        for (File file : sourceDirectory.listFiles()) {
            if (file.isDirectory()) {
                sync(file.getPath().toString(), Paths.get(targetPath, file.getName()).toString());
            } else {
                Files.copy(file.toPath(), Paths.get(targetDirectory.getPath(), file.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void validateDirectory(File directory) throws IllegalArgumentException {
        if (directory.exists() && !directory.isDirectory()) {
            throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory");
        }
        if (directory.exists() && !directory.canRead()) {
            throw new IllegalArgumentException("Read access denied for " + directory.getAbsolutePath());
        }
        if (directory.exists() && !directory.canWrite()) {
            throw new IllegalArgumentException("Write access denied for " + directory.getAbsolutePath());
        }
    }
}
