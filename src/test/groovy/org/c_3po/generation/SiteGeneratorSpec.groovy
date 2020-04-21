package org.c_3po.generation

import org.c_3po.cmd.CmdArguments
import org.c_3po.io.Directories
import org.jsoup.Jsoup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import static org.c_3po.generation.SiteGenerationHelpers.generateSite

/**
 * Integration tests for site generation.
 */
class SiteGeneratorSpec extends Specification {
    @Shared Path srcDir = Paths.get("src/test/resources/test-project-src")
    @Shared Path destDir = Paths.get("src/test/resources/test-project-build")

    def setup() {
        SiteGenerationHelpers.ensureDestinationDirIsClean(destDir)
    }

    def "test that a not existing source directory isn't accepted"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.resolve("/foo").toString(), destDir.toString(), false, false)

        when:
        SiteGenerator.fromCmdArguments(cmdArguments);

        then:
        thrown(IllegalArgumentException)
    }

    def "test that a file as a source directory isn't accepted"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.resolve("blog.html").toString(), destDir.toString(), false, false)

        when:
        SiteGenerator.fromCmdArguments(cmdArguments);

        then:
        thrown(IllegalArgumentException)
    }

    def "test that result-ignorables are not put into the destination directory / output"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false, false)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);

        when:
        siteGenerator.generate()

        then:
        Files.notExists(destDir.resolve("_layouts"))
    }

    def "test that standard C-3PO files are not put into the destination directory / output"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false, false)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);

        when:
        siteGenerator.generate()

        then:
        Files.notExists(destDir.resolve(".c3poignore"))
        Files.notExists(destDir.resolve(".c3posettings"))
    }

    def "is able to fingerprint assets, e.g. to make cache busting possible" () {
        setup:
        def shouldFingerprintAssets = true
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false, shouldFingerprintAssets)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);

        when:
        siteGenerator.generate()

        then:
        Files.exists(destDir.resolve("css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
        Files.exists(destDir.resolve("css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"))
    }

    def "allows to omit fingerprinting assets" () {
        setup:
        def shouldFingerprintAssets = false
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false, shouldFingerprintAssets)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);

        when:
        siteGenerator.generate()

        then:
        Files.notExists(destDir.resolve("css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"))
        Files.notExists(destDir.resolve("css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"))
    }

// NOTE: Inactive because generateSite under the hoods causes a full build and thus HTML files
//  are regenerted anyways which makes testing for the replacement of refs to old fingerprinted assets
//  pointless. Instead, the generation would need to be started in autobuild mode, but as described
//  in another inactive test already, SiteGenerator.generateOnChange is blocking and thus can't be run
//  in a unit test.
//  Should this ever change (e.g. by using Java's Executor framework?!), adapt the test below. Right now
//  this specific feature (replacing outdated fingerprinted asset refs) is merely covered by the
//  fingerprinting specs that ensure that substitutes are returned accordingly.
//
//    def "replaces refs to outdated fingerprinted assets" () {
//        given: "a copy of the site cause a source file is gonna change"
//        def srcDirClone = Files.createTempDirectory("c-3po_src-dir-for-specs_")
//        Directories.copyDir(srcDir, srcDirClone)
//
//        and: "the copy of the site is being generated with fingerprinting"
//        generateSite(srcDirClone, destDir, true)
//
//        // TODO: Assert fingerprint ref before change
//
//        when: "source file is being changed"
//        Files.write(srcDirClone.resolve("css/main.scss"), ".button { color: blue; }".getBytes(), StandardOpenOption.APPEND)
//
//        and: "site is generated again"
//        generateSite(srcDirClone, destDir, true)
//
//        then: "ref to the old fingerprinted version of that file is replaced by a ref to the new one"
//        def doc = Jsoup.parse(destDir.resolve("blog.html").toFile(), "UTF-8")
//        def elements = doc.select("link[rel='stylesheet']")
//        assert elements.get(0).attr("href") == "/css/main.ce9a91e765963d1f568fbd4b7a7eedecab53889c.css"
//
//        cleanup:
//        srcDirClone.toFile().deleteDir()
//    }

    // TODO: test that result-ignorable triggers a build when being modified in autoBuild mode
    // TODO: test that newly added result-ignorable is cleaned up in destination directory

// NOTE: because of crappy autoBuild interface (generateOnFileChange is blocking, not shutdown) unit test is not possible yet
//    def "test that a result-ignorable still triggers a build in autoBuild mode when being modified"() {
//        setup:
//        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), true)
//        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);
//
//        when:
//        siteGenerator.generateOnFileChange()
//        destDir.resolve("about.html").toFile().delete();
//        assert Files.notExists(destDir.resolve("about.html"));
//        Files.write(destDir.resolve("_layouts/new-layout.html"), ["Line1", "Line2"])
//
//        then:
//        Files.exists(destDir.resolve("about.html"))
//
//        cleanup:
//        // TODO cleanup by requesting SiteGenerator to stop processing in autobuild mode
//    }
}
