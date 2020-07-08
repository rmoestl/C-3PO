package org.c_3po.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Value class holding command line arguments.
 */
public class CmdArguments {
    private static final Logger LOG = LoggerFactory.getLogger(CmdArguments.class);

    private final String sourceDirectory;
    private final String destinationDirectory;
    private final boolean autoBuild;
    private final boolean fingerprintAssets;
    private final boolean purgeCss;

    public CmdArguments(String sourceDirectory, String destinationDirectory, boolean autoBuild,
                        boolean fingerprintAssets, boolean purgeCss) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.autoBuild = autoBuild;
        this.fingerprintAssets = fingerprintAssets;
        this.purgeCss = purgeCss;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getDestinationDirectory() {
        return destinationDirectory;
    }

    public boolean isAutoBuild() {
        return autoBuild;
    }

    public boolean shouldFingerprintAssets() {
        return fingerprintAssets;
    }

    public boolean shouldPurgeUnusedCss() {
        return purgeCss;
    }

    public boolean validate() throws IOException {
        boolean validationResult = true;

        validationResult = isSrcAndDestNotTheSame();

        return validationResult;
    }

    @Override
    public String toString() {
        return "CmdArguments{" +
                "sourceDirectory='" + sourceDirectory + '\'' +
                ", destinationDirectory='" + destinationDirectory + '\'' +
                ", fingerprintAssets='" + fingerprintAssets + '\'' +
                ", purgeCss='" + purgeCss + '\'' +
                ", autoBuild=" + autoBuild +
                '}';
    }

    private boolean isSrcAndDestNotTheSame() throws IOException {
        boolean dirsAreTheSame;
        final Path srcPath = Paths.get(sourceDirectory);
        final Path destpath = Paths.get(destinationDirectory);

        if (Files.exists(srcPath) && Files.exists(destpath)) {
            dirsAreTheSame = Files.isSameFile(srcPath, destpath);
        } else {
            dirsAreTheSame = srcPath.equals(destpath);
        }

        if (dirsAreTheSame) {
            LOG.error("'src' and 'dest' locate the same directory, please use different directories");
        }
        return !dirsAreTheSame;
    }
}
