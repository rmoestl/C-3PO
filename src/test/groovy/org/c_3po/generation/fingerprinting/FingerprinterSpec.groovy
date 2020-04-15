package org.c_3po.generation.fingerprinting


import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import static org.c_3po.generation.SiteGenerationHelpers.ensureDestinationDirIsClean
import static org.c_3po.generation.SiteGenerationHelpers.generateSite

class FingerprinterSpec extends Specification {
    @Shared srcDir = Paths.get("src/test/resources/test-project-src")
    @Shared destDir = Paths.get("src/test/resources/test-project-build")
    @Shared cssDir = destDir.resolve("css")

    def setupSpec() {
        ensureDestinationDirIsClean(destDir)
        generateSite(srcDir, destDir)
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

    def "deletes fingerprinted files if their fingerprint is outdated" () {
        given: "an already fingerprinted css file"
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        when: "its content is being changed and fingerprinted again"
        Files.write(destDir.resolve("css/main.css"), ".button { color: blue; }".getBytes(), StandardOpenOption.APPEND)
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then: "the old fingerprinted version gets replaced by the new fingerprinted one"
        Files.exists(cssDir.resolve("main.3068d19b5816a4c201a3d72ca4e5e7433537b947.css"))
        Files.notExists(cssDir.resolve("main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
    }

    // TODO: Revert all fingerprinted files after each feature in a cleanup method
}
