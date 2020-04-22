package org.c_3po.generation.assets

import org.jsoup.Jsoup
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths

class ReplaceAssetsReferencesInDocSpec extends Specification {
    def assetSubstitutes = [ '/css/main.css': '/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css' ]
    def generatorSettings = new Properties()

    def setup() {
        def srcDir = Paths.get("src/test/resources/test-project-src")

        // TODO: If ever reading out settings is ever more than that, e.g. coercing default values,
        //  be sure to use the corresponding function that is called in application code as well.
        generatorSettings.load(Files.newInputStream(srcDir.resolve(".c3posettings")))
    }

    @Unroll
    def "replaces internal stylesheet references of type '#ref'" (String ref, String refPastReplacement) {
        given:
        def doc = createDoc(ref)

        when:
        AssetReferences.replaceAssetsReferences(doc, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == refPastReplacement

        where:
        ref | refPastReplacement
        "/css/main.css" | "/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "css/main.css" | "css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
        "https://example.com/css/main.css" | "https://example.com/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css"
    }

    // TODO: Test that refs to assets whose path isn't correct (main.css) shouldn't be replaced.

    @Unroll
    def "does not replace external stylesheet references of type '#ref'" (String ref) {
        given:
        def doc = createDoc(ref)

        when:
        AssetReferences.replaceAssetsReferences(doc, assetSubstitutes, generatorSettings)

        then:
        doc.select("link[rel='stylesheet']").get(0).attr("href") == ref

        where:
        // Note: the '_' is the way single column tables can be written
        ref | _
        "https://blog.example.com/css/main.css" | _
        "https://www.google.com/css/main.css" | _
    }

    def createDoc(String assetRef) {
        return Jsoup.parse("""\
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Foo</title>
          <link href="${assetRef}" rel="stylesheet">
        </head>
        <body></body>
        </html>
        """)
    }
}
