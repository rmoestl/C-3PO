package org.c_3po.generation

import org.c_3po.cmd.CmdArguments

class SiteGenerationHelpers {
    def static generateSite(srcDir, destDir, fingerprintAssets = true) {
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false, fingerprintAssets)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments)
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
