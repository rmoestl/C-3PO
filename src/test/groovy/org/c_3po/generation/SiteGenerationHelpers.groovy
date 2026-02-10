package org.c_3po.generation

import org.c_3po.cmd.CmdArguments

class SiteGenerationHelpers {
    public static final String TEST_PROJECT_SRC_DIR = "src/test/resources/test-project-src"
    public static final String TEST_PROJECT_DEST_DIR = "src/test/resources/test-project-build"
    public static final CmdArguments TEST_PROJECT_CMD_ARGS =
        new CmdArguments(TEST_PROJECT_SRC_DIR, TEST_PROJECT_DEST_DIR, false, false, false)
    public static final Configuration TEST_PROJECT_CONFIGURATION =
        Configuration.deriveFrom(TEST_PROJECT_CMD_ARGS)

    def static generateSite(srcDir, destDir, fingerprintAssets = true) {
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false, fingerprintAssets, false)
        def config = Configuration.deriveFrom(cmdArguments)
        def siteGenerator = SiteGenerator.from(config)
        siteGenerator.generate()
    }

    def static ensureDestinationDirIsClean(destDir) {
        def file = destDir.toFile()
        if (file.exists()) {
            def wasDeleted = file.deleteDir();
            if (!wasDeleted) {
                throw new RuntimeException("Failed to delete destination directory");
            }
        }
    }
}
