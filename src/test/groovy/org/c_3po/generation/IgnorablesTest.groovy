package org.c_3po.generation

import spock.lang.Specification

import java.nio.file.Paths

/**
 * Unit tests for {@link Ignorables}.
 */
class IgnorablesTest extends Specification {
    def "test that readCompleteIgnorables is returning only those files and directories that should be ignored completely"() {
        setup:
        def ignoreFilePath = Paths.get("src/test/resources/.c3po-ignore-IgnorablesTest")
        def completeIgnorables = Ignorables.readCompleteIgnorables(ignoreFilePath)
        def expectedList = ["temp files", "temp/*.md", ".idea", ".git"]

        expect:
        completeIgnorables.containsAll(expectedList)
        completeIgnorables.size() == expectedList.size()
    }

    def "test that readSitemapIgnorables is returning only those files and directories that should be ignored only when generating the sitemap"() {
        setup:
        def ignoreFilePath = Paths.get("src/test/resources/.c3po-ignore-IgnorablesTest")
        def completeIgnorables = Ignorables.readSitemapIgnorables(ignoreFilePath)
        def expectedList = ["private"]

        expect:
        completeIgnorables.containsAll(expectedList)
        completeIgnorables.size() == expectedList.size()
    }
}
