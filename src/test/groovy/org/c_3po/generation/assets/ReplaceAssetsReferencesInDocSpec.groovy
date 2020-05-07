package org.c_3po.generation.assets

import org.jsoup.Jsoup
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths

class ReplaceAssetsReferencesInDocSpec extends Specification {
    def assetSubstitutes = [
            '/css/main.css': '/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css',
            '/css/vendor/normalize.css': '/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css'
    ]
    def generatorSettings = new Properties()

    def setup() {
        def srcDir = Paths.get("src/test/resources/test-project-src")

        // TODO: If ever reading out settings is ever more than that, e.g. coercing default values,
        //  be sure to use the corresponding function that is called in application code as well.
        generatorSettings.load(Files.newInputStream(srcDir.resolve(".c3posettings")))
    }

    // TODO: Is there a difference between docURI being "/about/" and "/about" when it comes to resolve relative paths?

    @Unroll
    def "replaces absolute stylesheet references of type '#ref'" (String ref, String refPastReplacement) {
        given:
        def doc = createDoc(ref)
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == refPastReplacement

        where:
        ref | refPastReplacement
        "https://example.com/css/main.css" | "https://example.com/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "https://example.com/css/../css/main.css" | "https://example.com/css/../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "https://example.com/css/vendor/normalize.css" | "https://example.com/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"
        "https://example.com/css/vendor/../vendor/normalize.css" | "https://example.com/css/vendor/../vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"
    }

    @Unroll
    def "skips replacing absolute stylesheet refs to non-exiting assets like '#ref'" (String ref) {
        given:
        def doc = createDoc(ref)
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == ref

        where:
        ref | _
        "https://example.com/css/foo.css" | _
        "https://example.com/css/vendor/jquery-ui.css" | _
    }

    @Unroll
    def "replaces protocol-relative stylesheet references of type '#ref'" (String ref, String refPastReplacement) {
        given:
        def doc = createDoc(ref)
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == refPastReplacement

        where:
        ref | refPastReplacement
        "//example.com/css/main.css" | "//example.com/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "//example.com/css/../css/main.css" | "//example.com/css/../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "//example.com/css/vendor/normalize.css" | "//example.com/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"
        "//example.com/css/vendor/../vendor/normalize.css" | "//example.com/css/vendor/../vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"
    }

    @Unroll
    def "skips replacing protocol-relative stylesheet refs to non-exiting assets like '#ref'" (String ref) {
        given:
        def doc = createDoc(ref)
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == ref

        where:
        ref | _
        "//example.com/css/foo.css" | _
        "//example.com/css/vendor/jquery-ui.css" | _
    }

    @Unroll
    def "replaces root-relative stylesheet references of type '#ref'" (String ref, String refPastReplacement) {
        given:
        def doc = createDoc(ref)
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == refPastReplacement

        where:
        ref | refPastReplacement
        "/css/main.css" | "/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "/css/../css/main.css" | "/css/../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "/css/vendor/normalize.css" | "/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"
        "/css/vendor/../vendor/normalize.css" | "/css/vendor/../vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css"
    }

    @Unroll
    def "skips replacing root-relative stylesheet refs to non-exiting assets like '#ref'" (String ref) {
        given:
        def doc = createDoc(ref)
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == ref

        where:
        ref | _
        "/css/foo.css" | _
        "/css/vendor/jquery-ui.css" | _
    }

    @Unroll
    def "replaces relative refs of type '#ref' in docs with path '#docURI'" (String ref, String docURI,
                                                                             String refPastReplacement) {
        given:
        def doc = createDoc(ref)

        when:
        AssetReferences.replaceAssetsReferences(doc, URI.create(docURI), assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == refPastReplacement

        where:
        ref | docURI | refPastReplacement
        "css/main.css" | "/about.html" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "./css/main.css" | "/about.html" | "./css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "../css/main.css" | "/blog/a-blog-article.html" | "../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "./../css/main.css" | "/blog/a-blog-article.html" | "./../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "./../../css/main.css" | "/blog/2020/a-blog-article.html" | "./../../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "../../css/main.css" | "/blog/2020/a-blog-article.html" | "../../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
    }

    @Unroll
    def "factors in <base> element with href of type '#baseHref' when replacing relative refs" (String baseHref,
                                                                                                String ref,
                                                                                                String refPastReplacement) {
        given:
        def doc = createDoc(ref, baseHref)
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == refPastReplacement

        where:
        baseHref | ref | refPastReplacement
        "https://example.com/" | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "https://example.com/css" | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "https://example.com/css/" | "main.css" | "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "https://example.com/css/" | "./main.css" | "./main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "https://example.com/css/" | "../css/main.css" | "../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "//example.com/" | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "//example.com/css" | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "//example.com/css/" | "main.css" | "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "/" | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "/css" | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "/css/" | "main.css" | "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        ".." | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "../css" | "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "../css/" | "main.css" | "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "../css/" | "main.css" | "main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "../blog/" | "../css/main.css" | "../css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
    }

    @Unroll
    def "skips replacing relative refs to non-exiting assets like '#ref' in docs with path '#docURI'" (String ref,
                                                                                                       String docURI) {
        given:
        def doc = createDoc(ref)

        when:
        AssetReferences.replaceAssetsReferences(doc, URI.create(docURI), assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == ref

        where:
        docURI | ref
        "/index.html" | "css/foo.css"
        "/blog/a-blog-article.html" | "css/main.css"
    }

    def "skips replacing relative refs if <base> element refers to an external URI" () {
        given:
        def ref = "css/main.css"
        def doc = createDoc(ref, "https://google.com")
        def docURI = URI.create("/blog/a-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == ref
    }

    @Unroll
    def "skips replacing external stylesheet references of type '#ref'" (String ref) {
        given:
        def doc = createDoc(ref)
        def docURI = URI.create("/blog/2020/03/04/fine-blog-article.html")

        when:
        AssetReferences.replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == ref

        where:
        // Note: the '_' is the way single column tables can be written
        ref | _
        "https://blog.example.com/css/main.css" | _
        "https://www.google.com/css/main.css" | _
    }

    def createDoc(String assetRef, String baseHref = null) {
        def baseElem = baseHref ? """<base href="${baseHref}">""" : ""
        return Jsoup.parse("""\
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          ${baseElem}
          <title>Foo</title>
          <link href="${assetRef}" rel="stylesheet">
        </head>
        <body></body>
        </html>
        """)
    }
}
