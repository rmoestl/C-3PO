package org.c_3po.generation.fingerprinting

import org.c_3po.cmd.CmdArguments
import org.c_3po.generation.SiteGenerator
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Unit tests for {@Fingerprinter}.
 */
class FingerprinterTest extends Specification {
    @Shared Path srcDir = Paths.get("src/test/resources/test-project-src")
    @Shared Path destDir = Paths.get("src/test/resources/test-project-build")

    def "test that fingerprintStylesheets fingerprints CSS files"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments)
        def cssDir = destDir.resolve("css")
        siteGenerator.generate()

        when:
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then:
        Files.exists(cssDir.resolve("main.css"))
        Files.exists(cssDir.resolve("main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
    }

    def "test that fingerprintStylesheets fingerprints CSS files in sub directories of the root stylesheet dir"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments)
        def cssDir = destDir.resolve("css")
        siteGenerator.generate()

        when:
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then:
        Files.exists(cssDir.resolve("vendor/normalize.css"))
        Files.exists(cssDir.resolve("vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"))
    }

    // TODO: Keep it DRY, see SiteGeneratorTest.
    def cleanup() {
        def file = destDir.toFile()
        if (file.exists()) {
            def wasDeleted = file.deleteDir();
            if (!wasDeleted) {
                throw new RuntimeException("Failed to clean up directory");
            }
        }
    }
}
