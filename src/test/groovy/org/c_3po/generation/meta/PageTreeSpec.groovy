package org.c_3po.generation.meta

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths
import java.time.LocalDate

class PageTreeSpec extends Specification {
    @Shared def pageTree = new PageTree()
    @Shared def blogPage1 = newPage("/blog/1.html", "Foo", "2026-01-02")
    @Shared def blogPage2 = newPage("/blog/2.html", "Bar", "2026-01-29")
    @Shared def wrenchPage = newPage("/tools/wrench.html", "Wrench", "2025-07-21")
    @Shared def hammerPage = newPage("/tools/hammer.html", "Hammer", "2025-03-01")
    @Shared def brushPage = newPage("/tools/painter/brush.html", "Brush", "2024-04-27")

    def setupSpec() {
        pageTree.add(blogPage1)
        pageTree.add(blogPage2)
        pageTree.add(wrenchPage)
        pageTree.add(hammerPage)
        pageTree.add(brushPage)
    }

    def ".getPathsIn returns just pages contained in the given path"() {
        when:
        def result = pageTree.getPagesIn(Paths.get("/blog"))

        then:
        result.size() == 2
        result.contains(blogPage1)
        result.contains(blogPage2)
    }

    def ".getPathsIn does not return pages in sub-paths of given path"() {
        when:
        def result = pageTree.getPagesIn(Paths.get("/tools/painter"))

        then:
        result.size() == 1
        result.contains(brushPage)
    }

    def ".getPathsIn returns an empty list if no page exists with this parent path"() {
        when:
        def result = pageTree.getPagesIn(Paths.get("/tools/car-mechanic"))

        then:
        result != null
        result.size() == 0
    }

    def ".getPathsIn returns pages ordered by publish date in descending order"() {
        when:
        def result = pageTree.getPagesIn(Paths.get("/blog"))

        then:
        result.size() == 2
        result.get(0) == blogPage2
        result.get(1) == blogPage1
    }

    def ".getPathsIn returns pages with same publish date ordered by title (in ascending order) additionally"() {
        given:
        def pageTree = new PageTree()
        def fooPage = newPage("/foo.html", "Foo", "2026-01-02")
        def barPage = newPage("/bar.html", "Bar", "2026-01-02")
        def foxPage = newPage("/fox.html", "Far", "2026-01-04")
        pageTree.add(fooPage)
        pageTree.add(barPage)
        pageTree.add(foxPage)

        when:
        def result = pageTree.getPagesIn(Paths.get("/"))

        then:
        result.size() == 3
        result.get(0) == foxPage
        result.get(1) == barPage
        result.get(2) == fooPage
    }

    private static def newPage(String path, String title, String date) {
        return new Page(Paths.get(path), title, LocalDate.parse(date));
    }
}
