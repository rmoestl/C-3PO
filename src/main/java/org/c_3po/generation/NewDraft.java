package org.c_3po.generation;

import org.c_3po.cmd.CmdArguments;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewDraft {
    public static Path getFilePathFrom(CmdArguments cmdArgs) {
        return getFilePathFrom(cmdArgs, 0);
    }

    private static Path getFilePathFrom(CmdArguments cmdArgs, int suffixNumber) {
        Path srcDirPath = Paths.get(cmdArgs.getSourceDirectory(), cmdArgs.getNewDraftDir());
        String tag = cmdArgs.getNewDraftTag();

        // To avoid collisions. If 0, no infix.
        String numericalInfix = suffixNumber == 0 ? "" : "_" + suffixNumber;
        boolean tagIsSet = tag != null && !tag.isBlank();
        String fileName = tagIsSet
                ? "_draft_" + tag + numericalInfix + ".md"
                : "_draft" + numericalInfix + ".md";

        Path filePath = srcDirPath.resolve(fileName);

        return Files.exists(filePath)
                ? getFilePathFrom(cmdArgs, ++suffixNumber)
                : filePath;
    }
}
