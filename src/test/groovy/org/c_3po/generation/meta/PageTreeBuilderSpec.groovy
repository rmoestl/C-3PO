package org.c_3po.generation.meta

import org.c_3po.generation.Configuration
import org.c_3po.generation.SiteGenerationHelpers
import org.c_3po.generation.SiteGenerator
import spock.lang.Shared
import spock.lang.Specification

class PageTreeBuilderSpec extends Specification {
    @Shared Configuration configuration = SiteGenerationHelpers.TEST_PROJECT_CONFIGURATION
    @Shared PageTreeBuilder pageTreeBuilder = PageTreeBuilder.getInstance(SiteGenerator.from(configuration))
    @Shared PageTree pageTree = pageTreeBuilder.obtainFrom(configuration.getSourceDirectory())

    def "obtains a nested page tree from a specified source directory"() {
        expect:
        pageTree.getPagesIn("/").size() == 2
        pageTree.getPagesIn("/blog").size() == 2
    }

    def "omits C-3PO setting files in source directory root"() {
        when:
        def pages = pageTree.getPagesIn("/")

        then:
        pageListContains(pages, "/blog.html", "/about.html")
    }

    def "omits md-template.html files since they only exist to render Markdown documents in that folder"() {
        when:
        def pages = pageTree.getPagesIn("/blog")

        then:
        pageListContains(pages, "/blog/first-blog-post.html", "/blog/second-blog-post.html")
    }

    def "omits the _layouts folder since it is considered a result-ignorable"() {
        when:
        def pages = pageTree.getPagesIn("/_layouts")

        then:
        pages.size() == 0
    }

    def "omits asset folders since they don't contain any files that result in HTML pages"() {
        when:
        def pagesInImg = pageTree.getPagesIn("/img")
        def pagesInCss = pageTree.getPagesIn("/js")
        def pagesInJs = pageTree.getPagesIn("/css")

        then:
        pagesInImg.size() == 0
        pagesInCss.size() == 0
        pagesInJs.size() == 0
    }

    def "doesn't omit sitemap-ignorables since this would prevent generating index pages of private areas"() {
        when:
        def pages = pageTree.getPagesIn("/private")

        then:
        pageListContains(pages, "/private/secret-sauce.html")
    }

    private static def pageListContains(List<Page> pages, String... expUrls) {
        assert pages.size() == expUrls.size()
        pages.each { page ->
            assert expUrls.contains(page.url().toString())
        }
    }
}
