package org.c_3po.generation.assets;

import org.c_3po.io.FileFilters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class AssetReferences {
    private static final Logger LOG = LoggerFactory.getLogger(AssetReferences.class);

    // TODO: Instead of assetSubstitutes pass an object holding keys for stylesheet substitutes, image substitutes
    //  and so on. This will allow to be more efficient by knowing which substitutes are relevant
    //  for which type of reference.
    public static void replaceAssetsReferences(Path dir, Map<String, String> assetSubstitutes,
                                               Properties generatorSettings) throws IOException {
        // Replace references
        // TODO: Replace all refs in all docs in one pass
        // TODO: If site is built into a non-empty destination dir, also replace outdated fingerprinted refs, though
        //  that might be a feature of the calling code whose job is to supply the assetSubstitutes map.
        try (var htmlFiles = Files.newDirectoryStream(dir, FileFilters.htmlFilter)) {
            for (Path htmlFile : htmlFiles) {
                Document doc = Jsoup.parse(htmlFile.toFile(), "UTF-8");

                replaceStylesheetReferences(doc, assetSubstitutes, htmlFile, generatorSettings);

                Files.write(htmlFile, doc.outerHtml().getBytes());
            }
        }

        // Replace refs in sub directories
        try (var subDirs = FileFilters.subDirStream(dir)) {
            for (var subDir : subDirs) {
                replaceAssetsReferences(subDir, assetSubstitutes, generatorSettings);
            }
        }
    }

    // TODO: Also replace outdated fingerprinted asset refs. For example they can exist, if
    //  the site is only being built partially because just a static asset has changed and
    //  HTML files are not regenerated.
    private static void replaceStylesheetReferences(Document doc, Map<String, String> stylesheetSubstitutes,
                                                    Path docPath, Properties generatorSettings) {
        // TODO: Check if there any other way to reference a stylesheet?
        var elements = doc.select("link[rel='stylesheet']");
        for (org.jsoup.nodes.Element element : elements) {
            String href = element.attr("href");
            try {
                URI hrefURI = new URI(href);

                if (isAssetControlledByWebsite(hrefURI, new URI(generatorSettings.getProperty("baseUrl")))) {
                    String assetPath = translateToAssetPath(hrefURI, determineBaseURI(doc, generatorSettings));

                    String substitutePath = stylesheetSubstitutes.get(assetPath);
                    if (substitutePath != null) {

                        // Note: Replace the asset's name only and leave the URL untouched otherwise.
                        String assetFileName = Paths.get(assetPath).getFileName().toString();
                        String substituteFileName = Paths.get(substitutePath).getFileName().toString();

                        // TODO: Ensure only last occurrence is replaced since String.replace will replace all occurrences
                        //  since the asset file name could be part of the path as well, e.g. css/main.css/main.css
                        element.attr("href", href.replace(assetFileName, substituteFileName));
                    } else {
                        LOG.warn(String.format("Failed to substitute asset resource '%s' found in %s", href, docPath));
                    }
                }
            } catch (URISyntaxException ignored) {
            }
        }
    }

    /**
     * Determines if the given URI is controlled by the website being built.
     *
     * If the website's baseURL contains either a www or non-www host, www
     * and non-www assets are considered to be served by this origin.
     * If the baseURL does not contain a www or non-www host, thus contains
     * a sub-domain different than "www", the given URI is only considered
     * to be an internal asset if it's URI matches the same host.
     */
    private static boolean isAssetControlledByWebsite(URI hrefURI, URI baseURI) {

        // TODO: Finish implementation.
        boolean isURIIncludingHost = hrefURI.getHost() != null;
        if (isURIIncludingHost) {
            return hrefURI.getHost().equals(baseURI.getHost());
        } else {
            return true;
        }
    }

    private static String translateToAssetPath(final URI hrefURI, final URI baseURI) {
        /*
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

        if (isDocumentRelativeURI(hrefURI)) {

            // TODO: Take <base> into account
            // TODO: Add leading slash might be brittle but fact is that .getPath() after .resolve()
            //  does not render a leading slash if relative URI is of form `foo/bar.css` and baseURI
            //  does not have a path portion. It might be even more complicated like that.
            String uriPath = baseURI.resolve(hrefURI).getPath();
            return uriPath.startsWith("/") ? uriPath : "/" + uriPath;
        } else if (isProtocolRelativeURI(hrefURI)) {
            return hrefURI.getPath();
        } else if (isHostRelativeURI(hrefURI)) {
            return hrefURI.toString();
        } else if (hrefURI.isAbsolute()) {
            return hrefURI.getPath();
        } else {

            // TODO: Decide if this should be a warning. I think so.
            return hrefURI.toString();
        }
    }

    // TODO: Require the document's URI. What? Maybe the <base> tag is meant.
    private static URI determineBaseURI(Document doc, Properties generatorSettings) throws URISyntaxException {
        return new URI(generatorSettings.getProperty("baseUrl"));
    }

    private static boolean isHostRelativeURI(URI uri) {
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
