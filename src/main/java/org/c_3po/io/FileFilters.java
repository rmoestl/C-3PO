package org.c_3po.io;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A collection of file filters.
 */
public class FileFilters {
    public static DirectoryStream.Filter<Path> htmlFilter =
            entry -> Files.isRegularFile(entry) && entry.toFile().getName().endsWith(".html");
}
