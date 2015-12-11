package org.c_3po.generation

import org.c_3po.cmd.CmdArguments
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Integration tests for site generation.
 */
class SiteGeneratorTest extends Specification {
    @Shared Path srcDir = Paths.get("src/test/resources/test-project-src")
    @Shared Path destDir = Paths.get("src/test/resources/test-project-build")

    def "test that result-ignorables are not put into the destination directory / output"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);

        when:
        siteGenerator.generate()

        then:
        Files.notExists(destDir.resolve("_layouts"))
    }

    def "test that standard C-3PO files are not put into the destination directory / output"() {
        setup:
        def cmdArguments = new CmdArguments(srcDir.toString(), destDir.toString(), false)
        def siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);

        when:
        siteGenerator.generate()

        then:
        Files.notExists(destDir.resolve(".c3poignore"))
        Files.notExists(destDir.resolve(".c3posettings"))
    }

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
