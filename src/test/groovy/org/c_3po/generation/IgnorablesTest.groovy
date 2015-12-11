package org.c_3po.generation

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

/**
 * Unit tests for {@link Ignorables}.
 */
class IgnorablesTest extends Specification {
    @Shared ignoreFilePath = Paths.get("src/test/resources/test-project-src/.c3poignore")

    def "test that readCompleteIgnorables is returning only those files and directories that should be ignored completely"() {
        setup:
        def completeIgnorables = Ignorables.readCompleteIgnorables(ignoreFilePath)
        def expectedList = ["temp files", "temp/*.md", ".idea", ".git"]

        expect:
        completeIgnorables.containsAll(expectedList)
        completeIgnorables.size() == expectedList.size()
    }

    def "test that readSitemapIgnorables is returning only those files and directories that should be ignored only when generating the sitemap"() {
        setup:
        def completeIgnorables = Ignorables.readSitemapIgnorables(ignoreFilePath)
        def expectedList = ["private"]

        expect:
        completeIgnorables.containsAll(expectedList)
        completeIgnorables.size() == expectedList.size()
    }

    def "test that readResultIgnorables is returning only those files and directories whose output should be ignored but should still trigger builds in autoBuild mode when being modified"() {
        setup:
        def completeIgnorables = Ignorables.readResultIgnorables(ignoreFilePath)
        def expectedList = ["_layouts", "blog/cta-fragment.html"]

        expect:
        completeIgnorables.containsAll(expectedList)
        completeIgnorables.size() == expectedList.size()
    }
}
