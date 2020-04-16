package org.c_3po.generation.assets

import org.jsoup.Jsoup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

import static org.c_3po.generation.SiteGenerationHelpers.ensureDestinationDirIsClean
import static org.c_3po.generation.SiteGenerationHelpers.generateSite

class ReplaceAssetsReferencesSpec extends Specification {
    @Shared srcDir = Paths.get("src/test/resources/test-project-src")
    @Shared destDir = Paths.get("src/test/resources/test-project-build")

    def setupSpec() {
        ensureDestinationDirIsClean(destDir)
        generateSite(srcDir, destDir)
    }

    def "replaces stylesheet references" () {
        given:
        def assetSubstitutes = [
                '/css/main.css': '/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css',
                '/css/vendor/normalize.css': '/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css'
        ]

        // TODO: If ever reading out settings is more than that, e.g. coercing default values,
        //  be sure to use the corresponding function that is called in generation code as well.
        def generatorSettings = new Properties()
        generatorSettings.load(Files.newInputStream(srcDir.resolve(".c3posettings")))

        when:
        AssetReferences.replaceAssetsReferences(destDir, assetSubstitutes, generatorSettings)

        then:
        assertRefsReplacedIn("blog.html")
        assertRefsReplacedIn("about.html")
    }

    void assertRefsReplacedIn(htmlFileName) {
        def doc = Jsoup.parse(destDir.resolve(htmlFileName).toFile(), "UTF-8")
        def elements = doc.select("link[rel='stylesheet']")

        // Note: Assertions are strongly coupled to the order the stylesheets are referenced
        // in the test project. Thus, obviously Jsoup returns elements in document order.
        assert elements.get(0).attr("href") == '/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css'
        assert elements.get(1).attr("href") == '/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css'
    }
}
