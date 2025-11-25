package org.c_3po.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Value class holding command line arguments.
 * Note: Class could really benefit from builder pattern.
 */
public class CmdArguments {
    private static final Logger LOG = LoggerFactory.getLogger(CmdArguments.class);

    private final String sourceDirectory;
    private final String destinationDirectory;
    private final boolean autoBuild;
    private final boolean newDraftModeEnabled;
    private final String newDraftDir;
    private final String newDraftTag;
    private final boolean editModeEnabled;
    private final String fileToEdit;
    private final boolean fingerprintAssets;
    private final boolean purgeCss;

    public CmdArguments(String sourceDirectory, String destinationDirectory, boolean autoBuild,
                        boolean fingerprintAssets, boolean purgeCss) {
        this(sourceDirectory, destinationDirectory, autoBuild, false, "", "", false, "",
                fingerprintAssets, purgeCss);
    }

    public CmdArguments(String sourceDirectory, String destinationDirectory, boolean autoBuild,
                        boolean newDraftModeEnabled, String newDraftDir, String newDraftTag,
                        boolean editModeEnabled, String fileToEdit,
                        boolean fingerprintAssets, boolean purgeCss) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.autoBuild = autoBuild;
        this.newDraftModeEnabled = newDraftModeEnabled;
        this.newDraftDir = newDraftDir;
        this.newDraftTag = newDraftTag;
        this.editModeEnabled = editModeEnabled;
        this.fileToEdit = fileToEdit;
        this.fingerprintAssets = fingerprintAssets;
        this.purgeCss = purgeCss;
    }

    public static CmdArguments parse(String[] args) {
        String sourceDirectoryName = "";
        String destinationDirectoryName = "";
        boolean autoBuild = false;
        boolean newDraftModeEnabled = false;
        String newDraftDir = "";
        String newDraftTag = "";
        boolean editModeEnabled = false;
        String fileToEdit = "";
        boolean fingerprint = false;
        boolean purgeUnusedCss = false;

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            String prevArgument = i > 0 ? args[i - 1] : null;

            LOG.debug("Processing command line argument: {}", argument);

            if ("-src".equals(argument) && i < args.length - 1) {
                final String sourceDirArgument = args[i + 1];
                sourceDirectoryName = sourceDirArgument;
                i++;
            }

            if ("-dest".equals(argument) && i < args.length - 1) {
                final String destinationDirArgument = args[i + 1];
                destinationDirectoryName = destinationDirArgument;
                i++;
            }

            if ("-a".equals(argument)) {
                autoBuild = true;
            }

            if ("--fingerprint".equals(argument) || "-p".equals(argument)) {
                fingerprint = true;
            }

            if ("--purge-unused-css".equals(argument) || "-p".equals(argument)) {
                purgeUnusedCss = true;
            }

            if ("-n".equals(argument)) {
                newDraftModeEnabled = true;
            }

            if ("-n".equals(prevArgument) && Parsing.isValueArg(argument)) {
                newDraftDir = argument;
            }

            if ("-t".equals(prevArgument) && Parsing.isValueArg(argument)) {
                newDraftTag = argument;
            }

            if ("-e".equals(argument)) {
                editModeEnabled = true;
            }

            if ("-e".equals(prevArgument) && Parsing.isValueArg(argument)) {
                fileToEdit = argument;
            }
        }

        return new CmdArguments(sourceDirectoryName, destinationDirectoryName, autoBuild,
                newDraftModeEnabled, newDraftDir, newDraftTag, editModeEnabled, fileToEdit,
                fingerprint, purgeUnusedCss);
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

    public boolean isNewDraftModeEnabled() {
        return newDraftModeEnabled;
    }

    public String getNewDraftDir() {
        return newDraftDir;
    }

    public String getNewDraftTag() {
        return newDraftTag;
    }

    public boolean isEditModeEnabled() {
        return editModeEnabled;
    }

    public String getFileToEdit() {
        return fileToEdit;
    }

    public boolean shouldFingerprintAssets() {
        return fingerprintAssets;
    }

    public boolean shouldPurgeUnusedCss() {
        return purgeCss;
    }

    public boolean validate() throws IOException {
        return isSrcAndDestNotTheSame()
                && (!isNewDraftModeEnabled() || isNewDraftDirValid())
                && (!isEditModeEnabled() || doesFileToEditExist());
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

    private boolean isNewDraftDirValid() {
        return isNewDraftDirSet() && doesNewDraftDirExist();
    }

    private boolean isNewDraftDirSet() {
        var newDraftDirSet = newDraftDir != null && !newDraftDir.isBlank();
        if (!newDraftDirSet) {
            LOG.error("Directory that should contain the new draft is not set. Supply it with " +
                    "`-n <path>` whereas path needs to be an existing directory in source directory.");
        }
        return newDraftDirSet;
    }

    private boolean doesNewDraftDirExist() {
        var dirExists = Files.exists(Paths.get(sourceDirectory, newDraftDir));
        if (!dirExists) {
            LOG.error("Directory  '{}' that should contain the new draft does not exist" +
                            " within source directory '{}'", newDraftDir, sourceDirectory);
        }
        return dirExists;
    }

    private boolean doesFileToEditExist() {
        var fileExists = Files.exists(Paths.get(sourceDirectory, fileToEdit));
        if (!fileExists) {
            LOG.error("File '{}' does not exist. Edit mode `-e <file-path>` requires " +
                            "`file-path` to exist.", fileToEdit);
        }
        return fileExists;
    }

    public void logArguments() {
        LOG.debug("src (source directory) is: {}", getSourceDirectory());
        LOG.debug("dest (destination directory) is: {}", getDestinationDirectory());
        LOG.debug("autoBuild is: {}", isAutoBuild());
        LOG.debug("newDraftModeEnabled is: {}", isNewDraftModeEnabled());
        LOG.debug("newDraftTag is: {}", getNewDraftTag());
        LOG.debug("editModeEnabled is: {}", isEditModeEnabled());
        LOG.debug("fileToEdit is: {}", getFileToEdit());
        LOG.debug("fingerprint is: {}", shouldFingerprintAssets());
        LOG.debug("purgeUnusedCss is: {}", shouldPurgeUnusedCss());
    }

    private static class Parsing {
        private static boolean isValueArg(String argument) {
            return !isFlagArg(argument);
        }

        private static boolean isFlagArg(String argument) {
            return argument.startsWith("-") || argument.startsWith("--");
        }
    }
}
