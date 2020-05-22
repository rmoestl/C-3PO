package org.c_3po.generation.assets

import org.c_3po.io.Directories
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

import static org.c_3po.generation.SiteGenerationHelpers.ensureDestinationDirIsClean
import static org.c_3po.generation.SiteGenerationHelpers.generateSite
import static org.c_3po.generation.assets.AssetReferencesHelpers.queryStylesheetElems

class ReplaceAssetsReferencesInDirSpec extends Specification {
    @Shared srcDir = Paths.get("src/test/resources/test-project-src")
    @Shared destDir = Paths.get("src/test/resources/test-project-build")

    def assetSubstitutes = [
            '/css/main.css': '/css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css',
            '/css/vendor/normalize.css': '/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css',
            '/js/main.js': '/js/main.44782b626616c6098994363811a6014c6771c5d5.js',
            '/js/vendor/jquery.js': '/js/vendor/jquery.083f0c5df3398060df50f99d59edf31127720da0.js',
            '/img/logo.svg': '/img/logo.dd6c240331f12aa6489f3757b023b1b7866a17cc.svg'
    ]
    def generatorSettings = new Properties()
    def destDirClone = Files.createTempDirectory("c-3po_dest-dir-for-specs_")

    def setupSpec() {
        ensureDestinationDirIsClean(destDir)
        generateSite(srcDir, destDir, false)
    }

    def setup() {
        // Clone dir so that each feature works on a clean slate
        Directories.copyDir(destDir, destDirClone)

        // TODO: If ever reading out settings is ever more than that, e.g. coercing default values,
        //  be sure to use the corresponding function that is called in application code as well.
        generatorSettings.load(Files.newInputStream(srcDir.resolve(".c3posettings")))
    }

    def "replaces asset references in HTML within a given directory" () {
        when:
        AssetReferences.replaceAssetsReferences(destDirClone, assetSubstitutes, generatorSettings)

        then:
        assertRefsReplacedIn("blog.html")
        assertRefsReplacedIn("about.html")
    }

    def "replaces asset references in HTML files located in sub directories" () {
        when:
        AssetReferences.replaceAssetsReferences(destDirClone, assetSubstitutes, generatorSettings)

        then:
        assertRefsReplacedIn("blog/first-blog-post.html")
    }

    def "factors in a HTML document's path when replacing relative asset refs" () {
        given: "an asset referenced with a relative URI"
        assertAssetsWithRelativeURIIn("blog.html")
        assertAssetsWithRelativeURIIn("about.html")
        assertAssetsWithRelativeURIIn("blog/first-blog-post.html")

        when:
        AssetReferences.replaceAssetsReferences(destDirClone, assetSubstitutes, generatorSettings)

        then:
        assertRefsReplacedIn("blog.html")
        assertRefsReplacedIn("about.html")
        assertRefsReplacedIn("blog/first-blog-post.html")
    }

    def cleanup() {
        destDirClone.toFile().deleteDir()
    }

    void assertAssetsWithRelativeURIIn(htmlFilePath) {
        def elements = queryStylesheetElems(destDirClone.resolve(htmlFilePath))
        assert elements.get(0).attr("href") == 'css/main.css'
    }

    void assertRefsReplacedIn(htmlFilePath) {
        AssetReferencesHelpers.assertRefsReplacedIn(destDirClone.resolve(htmlFilePath))
    }
}
