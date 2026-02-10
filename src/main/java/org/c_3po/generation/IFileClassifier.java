package org.c_3po.generation;

import java.nio.file.Path;

public interface IFileClassifier {
    boolean isCompleteIgnorable(Path file);

    boolean isResultIgnorable(Path file);

    boolean isHTML(Path file);

    boolean isMarkdown(Path file);
}
