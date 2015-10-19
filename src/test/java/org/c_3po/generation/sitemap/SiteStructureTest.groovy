package org.c_3po.generation.sitemap

import spock.lang.Specification

import java.nio.file.Paths

/**
 * Created by robert on 19.10.2015.
 */
class SiteStructureTest extends Specification {
    def "test that .add throws NPE"() {
        setup:
        def baseUrl = "http://yodaconditions.net"
        def siteStructure = SiteStructure.getInstance(baseUrl)

        when:
        siteStructure.add(null);

        then:
        thrown(NullPointerException)
    }

    def "test that .add throws IllegalArgumentException when path is absolute"() {
        setup:
        def baseUrl = "http://yodaconditions.net"
        def siteStructure = SiteStructure.getInstance(baseUrl)
        def absolutePath = Paths.get("/d/Programmierung/YodaConditions/development/site/index.html").toAbsolutePath()

        when:
        siteStructure.add(absolutePath)

        then:
        thrown(IllegalArgumentException)
    }

    def "test that toUrls doesn't produces Urls with double slashes after baseUrl"(String baseUrl, String pagePath) {
        setup:
        def siteStructure = SiteStructure.getInstance(baseUrl)

        when:
        siteStructure.add(Paths.get(pagePath))
        def urls = siteStructure.toUrls()

        then:
        urls.size() == 1
        urls.get(0) != null
        urls.get(0).equals("http://yodaconditions.net/resources/seo-links.html")

        where:
        baseUrl | pagePath
        "http://yodaconditions.net/" | "resources/seo-links.html"
        "http://yodaconditions.net" | "resources/seo-links.html"
    }

    def "test that toUrls retrieves all previously added paths prefixed with set baseUrl"() {
        setup:
        def baseUrl = "http://yodaconditions.net"
        def siteStructure = SiteStructure.getInstance(baseUrl)
        def pages = ["index.html", "about.html", "resources/podcasts.html", "resources/seo-links.html", "startups/index.html"]

        when:
        pages.each({page -> siteStructure.add(Paths.get(page))})
        def urls = siteStructure.toUrls()

        then:
        urls.size() == 5
        pages.each({page -> urls.contains("http://yodaconditions.net/" + page)})
    }
}
