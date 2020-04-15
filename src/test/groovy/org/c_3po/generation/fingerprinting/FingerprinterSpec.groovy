package org.c_3po.generation.fingerprinting

import org.c_3po.cmd.CmdArguments
import org.c_3po.generation.SiteGenerator
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

class FingerprinterSpec extends Specification {
    @Shared destDir = Paths.get("src/test/resources/test-project-build")
    @Shared cssDir = destDir.resolve("css")

    def setupSpec() {
        def srcDir = Paths.get("src/test/resources/test-project-src")
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments)
        siteGenerator.generate()
    }

    def "fingerprints a css file" () {
        when:
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then:
        Files.exists(cssDir.resolve("main.css"))
        Files.exists(cssDir.resolve("main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
    }

    def "fingerprints a css file located in a subdir" () {
        when:
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then:
        Files.exists(cssDir.resolve("vendor/normalize.css"))
        Files.exists(cssDir.resolve("vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"))
    }

    def "does not fingerprint an already fingerprinted css file again" () {
        given: "fingerprinting is executed once"
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        when: "being executed a second time"
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then: "an already fingerprinted file is not fingerprinted again"
        Files.exists(cssDir.resolve("main.css"))
        Files.exists(cssDir.resolve("main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
        Files.notExists(cssDir.resolve(
                "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
    }

    def cleanupSpec() {

        // TODO: Keep it DRY, see SiteGeneratorTest.
        def file = destDir.toFile()
        if (file.exists()) {
            def wasDeleted = file.deleteDir();
            if (!wasDeleted) {
                throw new RuntimeException("Failed to clean up directory");
            }
        }
    }
}