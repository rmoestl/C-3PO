package org.c_3po.generation.assets

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import static org.c_3po.generation.SiteGenerationHelpers.ensureDestinationDirIsClean
import static org.c_3po.generation.SiteGenerationHelpers.generateSite
import static org.c_3po.generation.assets.FingerprinterSpecHelper.cleanupFingerprintedFiles

class FingerprinterSpec extends Specification {
    @Shared srcDir = Paths.get("src/test/resources/test-project-src")
    @Shared destDir = Paths.get("src/test/resources/test-project-build")
    @Shared cssDir = destDir.resolve("css")
    @Shared jsDir = destDir.resolve("js")

    def setupSpec() {
        ensureDestinationDirIsClean(destDir)
        generateSite(srcDir, destDir)
    }

    def setup() {
        cleanupFingerprintedFiles(cssDir)
    }

    def "fingerprints all css files within a given dir and its sub-dirs" () {
        when: "fingerprinting stylesheets is applied to a given dir"
        def substitutes = Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then: "fingerprinted versions of all found css files are created"
        Files.exists(cssDir.resolve("main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
        Files.exists(cssDir.resolve("vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"))

        and: "their original counterparts are not deleted"
        Files.exists(cssDir.resolve("main.css"))
        Files.exists(cssDir.resolve("vendor/normalize.css"))

        and: "the substitutes map has the same length as the number of found css files"
        substitutes.size() == 2

        and: "the substitutes map maps each css file to its fingerprinted counterpart"
        substitutes.get("/css/main.css") == "/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        substitutes.get("/css/vendor/normalize.css") ==
                "/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"
    }

    def "fingerprints all js files within a given dir and its sub-dirs" () {
        when: "fingerprinting js files is applied to a given dir"
        def substitutes = Fingerprinter.fingerprintJsFiles(jsDir, destDir)

        then: "fingerprinted versions of all found js files are created"
        Files.exists(jsDir.resolve("main.44782b626616c6098994363811a6014c6771c5d5.js"))
        Files.exists(jsDir.resolve("vendor/jquery.083f0c5df3398060df50f99d59edf31127720da0.js"))

        and: "their original counterparts are not deleted"
        Files.exists(jsDir.resolve("main.js"))
        Files.exists(jsDir.resolve("vendor/jquery.js"))

        and: "the substitutes map has the same length as the number of found js files"
        substitutes.size() == 2

        and: "the substitutes map maps each js file to its fingerprinted counterpart"
        substitutes.get("/js/main.js") == "/js/main.44782b626616c6098994363811a6014c6771c5d5.js"
        substitutes.get("/js/vendor/jquery.js") ==
                "/js/vendor/jquery.083f0c5df3398060df50f99d59edf31127720da0.js"
    }

    // TODO: Description on asset level doesn't fit the test contents that's based on CSS.
    //  It's yet unclear how the final API will look. At time of writing
    //  .fingerprintStylesheets was just a wrapper of the private .fingerprintAssets
    //  function. It's perfectly possible that those wrapper functions are going
    //  to be moved to the caller class and thus disappear from the fingerprinting API.
    def "does not fingerprint an already fingerprinted asset file again" () {
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

    // TODO: Description on asset level doesn't fit the test contents that's based on CSS.
    //  It's yet unclear how the final API will look. At time of writing
    //  .fingerprintStylesheets was just a wrapper of the private .fingerprintAssets
    //  function. It's perfectly possible that those wrapper functions are going
    //  to be moved to the caller class and thus disappear from the fingerprinting API.
    def "deletes fingerprinted files if their fingerprint is outdated" () {
        def newFilename = "main.3068d19b5816a4c201a3d72ca4e5e7433537b947.css"
        def oldFilename = "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"

        given: "an already fingerprinted css file"
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        when: "its content is being changed"
        Files.write(destDir.resolve("css/main.css"), ".button { color: blue; }".getBytes(), StandardOpenOption.APPEND)

        and: "fingerprinted again"
        Fingerprinter.fingerprintStylesheets(cssDir, destDir)

        then: "the new fingerprinted version gets created"
        Files.exists(cssDir.resolve(newFilename))

        and: "the old fingerprinted version gets deleted"
        Files.notExists(cssDir.resolve(oldFilename))
    }
}
