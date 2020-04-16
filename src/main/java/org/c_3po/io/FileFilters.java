package org.c_3po.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A collection of file filters and related helpers.
 */
public class FileFilters {
    public static DirectoryStream.Filter<Path> htmlFilter =
            entry -> Files.isRegularFile(entry) && entry.toFile().getName().endsWith(".html");

    public static DirectoryStream<Path> subDirStream(Path dir) throws IOException {
        return Files.newDirectoryStream(dir, entry -> Files.isDirectory(entry));
    }
}
