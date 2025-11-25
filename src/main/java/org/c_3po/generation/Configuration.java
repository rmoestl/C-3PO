package org.c_3po.generation;

import org.c_3po.cmd.CmdArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    private static final String C_3PO_SETTINGS_FILE_NAME = ".c3posettings";
    private static final String SETTING_BASEURL = "baseUrl";
    private static final String SETTING_SRC_DIR = "src";
    private static final String SETTING_DEST_DIR = "dest";
    private static final String SETTING_NODEJS_HOME = "nodejsHome";
    private static final String SETTING_PURIFYCSS_HOME = "purifycssHome";
    private static final String SETTING_PURIFYCSS_WHITELIST = "purifycssWhitelist";

    private final CmdArguments cmdArgs;
    private final Path srcDir;
    private final Path destDir;
    private final String newDraftDir;
    private final String fileToEdit;
    private final Properties settings;

    private Configuration(CmdArguments cmdArgs, Path srcDir, Path destDir, Properties settings) {
        this.cmdArgs = cmdArgs;
        this.srcDir = srcDir;
        this.destDir = destDir;
        this.newDraftDir = cmdArgs.getNewDraftDir();
        this.fileToEdit = cmdArgs.getFileToEdit();
        this.settings = settings;
    }

    /**
     * Derives a new {@link Configuration} from the passed {@link CmdArguments}
     * and properties file named `.c3posettings` either located in the directory
     * specified with `-src` on the command line or in the working dir.
     * @param cmdArgs an object representing command line arguments
     * @return a new {@link Configuration} object
     */
    public static Configuration deriveFrom(CmdArguments cmdArgs) {
        Properties settings = readSettings(cmdArgs.getSourceDirectory());

        var srcDir = coerceSourceDirectory(cmdArgs, settings);
        var destDir = coerceDestinationDirectory(cmdArgs, settings);

        return new Configuration(cmdArgs, srcDir, destDir, settings);
    }

    private static Properties readSettings(String cmdArgSourceDirectory) {

        // Either look for a settings file in source directory specified as a command
        // line arg or in the root of the working dir
        Path settingsParentDir = Paths.get(cmdArgSourceDirectory.isBlank()
                ? System.getProperty("user.dir")
                : cmdArgSourceDirectory);
        Path settingsFilePath = settingsParentDir.resolve(C_3PO_SETTINGS_FILE_NAME);

        Properties properties = new Properties();
        if (Files.exists(settingsFilePath)) {
            try {
                properties.load(Files.newInputStream(settingsFilePath));
            } catch (IOException e) {
                LOG.error("Failed to load settings from file '{}'", settingsFilePath);
            }
        } else {
            LOG.info("No settings file '{}' found.", C_3PO_SETTINGS_FILE_NAME);
        }

        return properties;
    }

    private static Path coerceSourceDirectory(CmdArguments cmdArguments, Properties settings) {
        var srcDir = "";

        if (cmdArguments.getSourceDirectory().isBlank()) {
            srcDir = settings.getProperty(SETTING_SRC_DIR, "");
        } else {
            srcDir = cmdArguments.getSourceDirectory();
        }

        return Paths.get(srcDir);
    }

    private static Path coerceDestinationDirectory(CmdArguments cmdArguments, Properties settings) {
        var destDir = "";

        if (cmdArguments.getDestinationDirectory().isBlank()) {
            destDir = settings.getProperty(SETTING_DEST_DIR, "");
        } else {
            destDir = cmdArguments.getDestinationDirectory();
        }

        return Paths.get(destDir);
    }

    public boolean validate() throws IOException {
        return isSrcAndDestNotTheSame()
                && (!isNewDraftModeEnabled() || isNewDraftDirValid())
                && (!isEditModeEnabled() || doesFileToEditExist());
    }

    private boolean isSrcAndDestNotTheSame() throws IOException {
        boolean dirsAreTheSame;

        if (Files.exists(srcDir) && Files.exists(destDir)) {
            dirsAreTheSame = Files.isSameFile(srcDir, destDir);
        } else {
            dirsAreTheSame = srcDir.equals(destDir);
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
        var dirExists = Files.exists(srcDir.resolve(newDraftDir));
        if (!dirExists) {
            LOG.error("Directory  '{}' that should contain the new draft does not exist" +
                    " within source directory '{}'", newDraftDir, srcDir);
        }
        return dirExists;
    }

    private boolean doesFileToEditExist() {
        var fileExists = Files.exists(srcDir.resolve(fileToEdit));
        if (!fileExists) {
            LOG.error("File '{}' does not exist. Edit mode `-e <file-path>` requires " +
                    "`file-path` to exist.", fileToEdit);
        }
        return fileExists;
    }

    public boolean validatePurifyCSSConfig() throws GenerationException {
        var nodejsHome = getNodeJSHome();
        var purifyCssHome = getPurifyCSSHome();

        if (nodejsHome == null || purifyCssHome == null) {
            if (nodejsHome == null) {
                LOG.error("Setting '{}', mandatory for purging unused CSS, is missing in '{}'",
                        SETTING_NODEJS_HOME, C_3PO_SETTINGS_FILE_NAME);
            }
            if (purifyCssHome == null) {
                LOG.error("Setting '{}', mandatory for purging unused CSS, is missing in '{}'",
                        SETTING_PURIFYCSS_HOME, C_3PO_SETTINGS_FILE_NAME);
            }
            throw new GenerationException("Abort build because purging unused CSS is active but not " +
                    "set up properly. See log for more details.");
        } else {
            return true;
        }
    }

    public Path getSourceDirectory() {
        return srcDir;
    }

    public Path getDestinationDirectory() {
        return destDir;
    }

    public boolean isAutoBuild() {
        return cmdArgs.isAutoBuild();
    }

    public boolean isNewDraftModeEnabled() {
        return cmdArgs.isNewDraftModeEnabled();
    }

    public String getNewDraftDir() {
        return cmdArgs.getNewDraftDir();
    }

    public String getNewDraftTag() {
        return cmdArgs.getNewDraftTag();
    }

    public boolean isEditModeEnabled() {
        return cmdArgs.isEditModeEnabled();
    }

    public String getFileToEdit() {
        return cmdArgs.getFileToEdit();
    }

    public boolean shouldFingerprintAssets() {
        return cmdArgs.shouldFingerprintAssets();
    }

    public boolean shouldPurgeUnusedCss() {
        return cmdArgs.shouldPurgeUnusedCss();
    }

    public String getBaseUrl() {
        return settings.getProperty(SETTING_BASEURL);
    }

    public String getNodeJSHome() {
        return settings.getProperty(SETTING_NODEJS_HOME);
    }

    public String getPurifyCSSHome() {
        return settings.getProperty(SETTING_PURIFYCSS_HOME);
    }

    public String getPurifyCSSWhitelist() {
        return settings.getProperty(SETTING_PURIFYCSS_WHITELIST, "");
    }
}
