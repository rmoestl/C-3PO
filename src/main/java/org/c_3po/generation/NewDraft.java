package org.c_3po.generation;

import java.nio.file.Files;
import java.nio.file.Path;

public class NewDraft {
    public static Path getFilePathFrom(Configuration config) {
        return getFilePathFrom(config, 0);
    }

    private static Path getFilePathFrom(Configuration config, int suffixNumber) {
        Path parentDir = config.getSourceDirectory().resolve(config.getNewDraftDir());
        String tag = config.getNewDraftTag();

        // To avoid collisions. If 0, no infix.
        String numericalInfix = suffixNumber == 0 ? "" : "_" + suffixNumber;
        boolean tagIsSet = tag != null && !tag.isBlank();
        String fileName = tagIsSet
                ? "_draft_" + tag + numericalInfix + ".md"
                : "_draft" + numericalInfix + ".md";

        Path filePath = parentDir.resolve(fileName);

        return Files.exists(filePath)
                ? getFilePathFrom(config, ++suffixNumber)
                : filePath;
    }
}
