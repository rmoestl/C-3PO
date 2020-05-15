package org.c_3po.generation.assets;

import org.c_3po.io.FileFilters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class AssetReferences {
    private static final Logger LOG = LoggerFactory.getLogger(AssetReferences.class);

    /**
     * Replaces asset references in the supplied {@link Jsoup} document.
     */
    public static void replaceAssetsReferences(Document doc, URI docURI, Map<String, String> assetSubstitutes,
                                               Properties generatorSettings) {
        replaceStylesheetReferences(doc, docURI, assetSubstitutes, generatorSettings);
    }

    // TODO: Instead of assetSubstitutes pass an object holding keys for stylesheet substitutes, image substitutes
    //  and so on. This will allow to be more efficient by knowing which substitutes are relevant
    //  for which type of reference.

    /**
     * Replaces asset references in all HTML files found in supplied dir and sub dirs.
     */
    public static void replaceAssetsReferences(Path dir, Map<String, String> assetSubstitutes,
                                               Properties generatorSettings) throws IOException {
        replaceAssetsReferences(dir, dir, assetSubstitutes, generatorSettings);
    }

    /**
     * Implementation method for replacing asset references.
     * @param dir dir containing HTML files to process
     * @param rootDir root directory of the site needed to calculate
     *                the path of HTML files in order to properly resolve
     *                relative asset refs
     */
    private static void replaceAssetsReferences(Path dir, Path rootDir, Map<String, String> assetSubstitutes,
                                               Properties generatorSettings) throws IOException {
        // Replace references
        // TODO: Replace all refs in all docs in one pass
        // TODO: If site is built into a non-empty destination dir, also replace outdated fingerprinted refs, though
        //  that might be a feature of the calling code whose job is to supply the assetSubstitutes map.
        try (var htmlFiles = Files.newDirectoryStream(dir, FileFilters.htmlFilter)) {
            for (Path htmlFile : htmlFiles) {
                Document doc = Jsoup.parse(htmlFile.toFile(), "UTF-8");
                URI docURI = URI.create(rootDir.relativize(dir).resolve(htmlFile.getFileName()).toString());

                LOG.debug(String.format("Replacing asset references in '%s'", htmlFile));
                replaceAssetsReferences(doc, docURI, assetSubstitutes, generatorSettings);

                Files.write(htmlFile, doc.outerHtml().getBytes());
            }
        }

        // Replace refs in sub directories
        try (var subDirs = FileFilters.subDirStream(dir)) {
            for (var subDir : subDirs) {
                replaceAssetsReferences(subDir, rootDir, assetSubstitutes, generatorSettings);
            }
        }
    }

    // TODO: Also replace outdated fingerprinted asset refs. For example they can exist, if
    //  the site is only being built partially because just a static asset has changed and
    //  HTML files are not regenerated.
    private static void replaceStylesheetReferences(Document doc, URI docURI, Map<String, String> stylesheetSubstitutes,
                                                    Properties generatorSettings) {
        // Note: According to https://html.spec.whatwg.org/#interactions-of-styling-and-scripting,
        // `<link rel="stylesheet">` is the only way to load an external stylesheet.
        var elements = doc.select("link[rel='stylesheet']");
        for (org.jsoup.nodes.Element element : elements) {
            String href = element.attr("href");
            var stylesheetURI = URI.create(href);
            var websiteBaseURI = URI.create(generatorSettings.getProperty("baseUrl"));
            var docBaseURI = determineDocBaseURI(docURI, doc);
            if (isAssetControlledByWebsite(stylesheetURI, websiteBaseURI, docBaseURI)) {
                String assetPath = translateToAssetPath(stylesheetURI, docBaseURI);

                String substitutePath = stylesheetSubstitutes.get(assetPath);
                if (substitutePath != null) {

                    // Note: Replace the asset's name only and leave the URL untouched otherwise.
                    String assetFileName = Paths.get(assetPath).getFileName().toString();
                    String substituteFileName = Paths.get(substitutePath).getFileName().toString();

                    // TODO: Ensure only last occurrence is replaced since String.replace will replace all occurrences
                    //  since the asset file name could be part of the path as well, e.g. css/main.css/main.css
                    element.attr("href", href.replace(assetFileName, substituteFileName));
                } else {
                    LOG.warn(String.format("Failed to substitute asset resource '%s'", href));
                }
            }
        }
    }

    /**
     * Determines if the given URI is controlled by the website being built.
     *
     * A resource is considered to be controlled by the website if
     *   - (i) it is a relative URI whose base is not external
     *     if a valid <base> element exists.
     *   - (ii) it is an absolute URI whose host part exactly matches
     *     the host part of the website base URI, meaning that example.com
     *     and www.example.com are considered to be different sites.
     */
    private static boolean isAssetControlledByWebsite(URI hrefURI, URI websiteBaseURI, URI docBaseURI) {
        var hrefURIHasHost = hrefURI.getHost() != null;
        var docBaseURIHasHost = docBaseURI.getHost() != null;
        if (hrefURIHasHost) {
            return hrefURI.getHost().equals(websiteBaseURI.getHost());
        } else if (isDocumentRelativeURI(hrefURI) && docBaseURIHasHost) {
            return docBaseURI.getHost().equals(websiteBaseURI.getHost());
        } else {
            return true;
        }
    }

    private static String translateToAssetPath(URI assetRefURI, URI docBaseURI) {
        /*
            TODO: Turn the comment into a function description
            The difficulty is to look at an URL and identify which
            asset is referenced by it.

            Types of URLs:

            https://example.com/index.html ==> Absolute URL
            //example.com/index.html ==> Implicit schema absolute URL, or better known as protocol-relative URI
            /css/main.css ==> Implicit schema and host absolute URL
            css/main.css ==> Document-relative URL

            Specs
            - Absolute URLs and implicit schema absolute URLs that reference the host either with or without www
            subdomain are considered to be resources held by the website that is being built.
            - Absolute URLs and implicit schema absolute URLs whose root domain is the same but have a different
            subdomain, e.g. blog.example.com are considered to not be held by this website. They are treated as foreign
            domains.
            - Any other absolute URLs are considered to be resources from third parties and thus are not replaced.

            - An absolute URL with implicit schema and host is the easiest. It should only be normalized and then be
            compared if any asset path is matching.

            - A relative URL is relative to the document it is referenced by. So, I'll need to construct the URI
            of the document and then `resolve()` the relative URLs. Then query the path portion via `getPath()`
            to obtain the asset path.
            - For a relative URL, always see if there are `<base>` elements in the parent document. Before calling
            resolve, be sure to resolve (?!) the base elements' `href` value. If there are multiple base elements,
            use the first one that has an href attribute.
            See https://html.spec.whatwg.org/multipage/urls-and-fetching.html#document-base-url.
        */

        if (isDocumentRelativeURI(assetRefURI)) {
            var assetPath = docBaseURI.resolve(assetRefURI).normalize().getPath();

            // TODO: Add leading slash might be brittle but fact is that .getPath() after .resolve()
            //  does not render a leading slash if relative URI is of form `foo/bar.css` and baseURI
            //  does not have a path portion. It might be even more complicated than that.
            return assetPath.startsWith("/") ? assetPath : "/" + assetPath;
        } else if (isProtocolRelativeURI(assetRefURI)) {
            return assetRefURI.normalize().getPath();
        } else if (isRootRelativeURI(assetRefURI)) {
            return assetRefURI.normalize().toString();
        } else if (assetRefURI.isAbsolute()) {
            return assetRefURI.normalize().getPath();
        } else {

            // TODO: Decide if this should be a warning. I think so.
            return assetRefURI.toString();
        }
    }

    /**
     * Determines the effective base URI of the document that is a result
     * of the document's URI and an optional <base> element included in
     * the markup itself.
     *
     * @param docURI the URI of the document
     * @param doc the document itself that may include a <base> element
     * @return the effective document's base URI against which
     * relative sub-resources ought to be resolved
     */
    private static URI determineDocBaseURI(URI docURI, Document doc) {

        // Note: The first base element with a href attribute is considered
        // valid in HTML. See https://developer.mozilla.org/en-US/docs/Web/HTML/Element/base.
        var baseElem = doc.selectFirst("base[href]");
        return baseElem != null ? docURI.resolve(baseElem.attr("href")) : docURI;
    }

    private static boolean isRootRelativeURI(URI uri) {
        return uri.getHost() == null && uri.toString().startsWith("/");
    }

    private static boolean isProtocolRelativeURI(URI uri) {
        return uri.getScheme() == null && uri.getHost() != null && uri.toString().startsWith("//");
    }

    private static boolean isDocumentRelativeURI(URI uri) {
        if (uri.getHost() != null) {
            return false;
        } else {
            return !uri.getPath().startsWith("/");
        }
    }
}
