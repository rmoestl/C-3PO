package org.c_3po.generation.assets

import org.jsoup.Jsoup
import org.jsoup.select.Elements

class AssetReferencesHelpers {
    static void assertRefsReplacedIn(htmlFilePath) {

        // Note: Assertions are strongly coupled to the order the assets are referenced
        // in the test project. Thus, obviously Jsoup returns elements in document order.
        def stylesheetElems = queryStylesheetElems(htmlFilePath)
        assert stylesheetElems.get(0).attr("href") == 'css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css'
        assert stylesheetElems.get(1).attr("href") == '/css/vendor/normalize.05802ba9503c8a062ee85857fc774d41e96d3a80.css'

        def jsElems = queryJsElems(htmlFilePath)
        assert jsElems.get(0).attr("src") == '/js/vendor/jquery.083f0c5df3398060df50f99d59edf31127720da0.js'
        assert jsElems.get(1).attr("src") == '/js/main.44782b626616c6098994363811a6014c6771c5d5.js'

        def imgElems = queryImgElems(htmlFilePath)
        assert imgElems.get(0).attr("src") == '/img/logo.dd6c240331f12aa6489f3757b023b1b7866a17cc.svg'
    }

    static Elements queryStylesheetElems(htmlFilePath) {
        def doc = Jsoup.parse(htmlFilePath.toFile(), "UTF-8")
        return doc.select("link[rel='stylesheet']")
    }

    static Elements queryJsElems(htmlFilePath) {
        def doc = Jsoup.parse(htmlFilePath.toFile(), "UTF-8")
        return doc.select("script[src]")
    }

    static Elements queryImgElems(htmlFilePath) {
        def doc = Jsoup.parse(htmlFilePath.toFile(), "UTF-8")
        return doc.select("img[src]")
    }
}
