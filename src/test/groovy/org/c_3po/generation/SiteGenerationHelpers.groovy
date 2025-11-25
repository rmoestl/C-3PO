package org.c_3po.generation

import org.c_3po.cmd.CmdArguments

class SiteGenerationHelpers {
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
