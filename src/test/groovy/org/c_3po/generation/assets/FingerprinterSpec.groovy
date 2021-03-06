package org.c_3po.generation.assets

import org.c_3po.io.Directories
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
    @Shared imgDir = destDir.resolve("img")

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

    def "fingerprints all images (jpg, png, gif, svg, webp) within a given dir and its sub-dirs" () {
        when: "fingerprinting images is applied to a given dir"
        def substitutes = Fingerprinter.fingerprintImageFiles(imgDir, destDir)

        then: "fingerprinted versions of all found images are created"
        Files.exists(imgDir.resolve("picture.e53496215f3b967267859fd2b108e29dbffc555c.jpg"))
        Files.exists(imgDir.resolve("artwork/graphic.d2213fbc994febae5ffd5306b18870bb72ba0bfb.png"))
        Files.exists(imgDir.resolve("fun/cat.716d7ad2ae0a5dce264a9e7b5a7592047b1f0552.gif"))
        Files.exists(imgDir.resolve("logo.dd6c240331f12aa6489f3757b023b1b7866a17cc.svg"))
        Files.exists(imgDir.resolve("optimized-picture.bf2b5a37c6d4f8e440d3b263b950a452581d0beb.webp"))

        and: "their original counterparts are not deleted"
        Files.exists(imgDir.resolve("picture.jpg"))
        Files.exists(imgDir.resolve("artwork/graphic.png"))
        Files.exists(imgDir.resolve("fun/cat.gif"))
        Files.exists(imgDir.resolve("logo.svg"))
        Files.exists(imgDir.resolve("optimized-picture.webp"))

        and: "the substitutes map has the same length as the number of found images"
        substitutes.size() == 5

        and: "the substitutes map maps each image to its fingerprinted counterpart"
        substitutes.get("/img/picture.jpg") == "/img/picture.e53496215f3b967267859fd2b108e29dbffc555c.jpg"
        substitutes.get("/img/artwork/graphic.png") ==
                "/img/artwork/graphic.d2213fbc994febae5ffd5306b18870bb72ba0bfb.png"
        substitutes.get("/img/fun/cat.gif") == "/img/fun/cat.716d7ad2ae0a5dce264a9e7b5a7592047b1f0552.gif"
        substitutes.get("/img/logo.svg") == "/img/logo.dd6c240331f12aa6489f3757b023b1b7866a17cc.svg"
        substitutes.get("/img/optimized-picture.webp") ==
                "/img/optimized-picture.bf2b5a37c6d4f8e440d3b263b950a452581d0beb.webp"
    }

    def "ignores case of file name extensions when fingerprinting assets" () {
        given: "a dest dir clone, in which files can be added without the need to delete " +
                "those files prior other specs in this suite"
        def destDirClone = Files.createTempDirectory("c-3po_dest-dir-for-specs_")
        Directories.copyDir(destDir, destDirClone)

        and: "asset with varying file extension capitalization"
        def cssDirClone = destDirClone.resolve("css")
        def jsDirClone = destDirClone.resolve("js")
        def imgDirClone = destDirClone.resolve("img")

        assert Files.exists(cssDirClone.resolve("main.css"))
        Files.copy(cssDirClone.resolve("main.css"), cssDirClone.resolve("main1.CSS"))
        Files.copy(cssDirClone.resolve("main.css"), cssDirClone.resolve("main2.cSS"))

        assert Files.exists(jsDirClone.resolve("main.js"))
        Files.copy(jsDirClone.resolve("main.js"), jsDirClone.resolve("main1.JS"))
        Files.copy(jsDirClone.resolve("main.js"), jsDirClone.resolve("main2.jS"))

        assert Files.exists(imgDirClone.resolve("picture.jpg"))
        Files.copy(imgDirClone.resolve("picture.jpg"), imgDirClone.resolve("picture1.JPG"))
        Files.copy(imgDirClone.resolve("picture.jpg"), imgDirClone.resolve("picture2.jPg"))

        when: "fingerprinting the assets"
        Fingerprinter.fingerprintStylesheets(cssDirClone, destDirClone)
        Fingerprinter.fingerprintJsFiles(jsDirClone, destDirClone)
        Fingerprinter.fingerprintImageFiles(imgDirClone, destDirClone)

        then: "fingerprinted versions of all those assets with varying file extension capitalization are created"
        filesExist(cssDirClone,
                "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css",
                "main1.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.CSS",
                "main2.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.cSS")
        filesExist(jsDirClone,
                "main.44782b626616c6098994363811a6014c6771c5d5.js",
                "main1.44782b626616c6098994363811a6014c6771c5d5.JS",
                "main2.44782b626616c6098994363811a6014c6771c5d5.jS")
        filesExist(imgDirClone,
                "picture.e53496215f3b967267859fd2b108e29dbffc555c.jpg",
                "picture1.e53496215f3b967267859fd2b108e29dbffc555c.JPG",
                "picture2.e53496215f3b967267859fd2b108e29dbffc555c.jPg")

        cleanup:
        destDirClone.toFile().deleteDir()
    }

    def "does not fingerprint an already fingerprinted stylesheet file again" () {
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

    def "deletes fingerprinted stylesheet files if their fingerprint is outdated" () {
        def oldFilename = "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        def newFilename = "main.3068d19b5816a4c201a3d72ca4e5e7433537b947.css"

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

    def "does not fingerprint an already fingerprinted JavaScript file again" () {
        given: "fingerprinting is executed once"
        Fingerprinter.fingerprintJsFiles(jsDir, destDir)

        when: "being executed a second time"
        Fingerprinter.fingerprintJsFiles(jsDir, destDir)

        then: "an already fingerprinted file is not fingerprinted again"
        Files.exists(jsDir.resolve("main.js"))
        Files.exists(jsDir.resolve("main.44782b626616c6098994363811a6014c6771c5d5.js"))
        Files.notExists(jsDir.resolve(
                "main.44782b626616c6098994363811a6014c6771c5d5.44782b626616c6098994363811a6014c6771c5d5.css"))
    }

    def "deletes fingerprinted JavaScript files if their fingerprint is outdated" () {
        def oldFilename = "main.44782b626616c6098994363811a6014c6771c5d5.js"
        def newFilename = "main.aa661d0bf7642c899dba93282d052dfef4645e86.js"

        given: "an already fingerprinted js file"
        Fingerprinter.fingerprintJsFiles(jsDir, destDir)

        when: "its content is being changed"
        Files.write(destDir.resolve("js/main.js"), "console.log('Foo!')".getBytes(), StandardOpenOption.APPEND)

        and: "fingerprinted again"
        Fingerprinter.fingerprintJsFiles(jsDir, destDir)

        then: "the new fingerprinted version gets created"
        Files.exists(jsDir.resolve(newFilename))

        and: "the old fingerprinted version gets deleted"
        Files.notExists(jsDir.resolve(oldFilename))
    }

    def "does not fingerprint an already fingerprinted image file again" () {
        given: "fingerprinting is executed once"
        Fingerprinter.fingerprintImageFiles(imgDir, destDir)

        when: "being executed a second time"
        Fingerprinter.fingerprintImageFiles(imgDir, destDir)

        then: "an already fingerprinted file is not fingerprinted again"
        Files.exists(imgDir.resolve("picture.jpg"))
        Files.exists(imgDir.resolve("picture.e53496215f3b967267859fd2b108e29dbffc555c.jpg"))
        Files.notExists(imgDir.resolve(
                "picture.e53496215f3b967267859fd2b108e29dbffc555c.e53496215f3b967267859fd2b108e29dbffc555c.jpg"))
    }

    def "deletes fingerprinted image files if their fingerprint is outdated" () {
        def oldFilename = "picture.e53496215f3b967267859fd2b108e29dbffc555c.jpg"
        def newFilename = "picture.b687f26535d07adea0f8dbe1863248f446bd5249.jpg"

        given: "an already fingerprinted image file"
        Fingerprinter.fingerprintImageFiles(imgDir, destDir)

        when: "its content is being changed"
        Files.write(destDir.resolve("img/picture.jpg"), "new pixels ;-)".getBytes(), StandardOpenOption.APPEND)

        and: "fingerprinted again"
        Fingerprinter.fingerprintImageFiles(imgDir, destDir)

        then: "the new fingerprinted version gets created"
        Files.exists(imgDir.resolve(newFilename))

        and: "the old fingerprinted version gets deleted"
        Files.notExists(imgDir.resolve(oldFilename))
    }

    def filesExist(dir, String... fileNames) {
        fileNames.each { fileName -> assert Files.exists(dir.resolve(fileName)) }
    }
}
