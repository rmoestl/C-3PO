package org.c_3po.generation.assets;

import org.c_3po.io.FileFilters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class AssetReferences {
    private static final Logger LOG = LoggerFactory.getLogger(AssetReferences.class);
    private static final Pattern FINGERPRINTED_ASSET_URI_PATTERN =
            Pattern.compile("^(.*)\\.[0123456789abcdef]{40}(\\.css)$");

    /**
     * Replaces asset references in the supplied {@link Jsoup} document.
     */
    public static void replaceAssetsReferencesInDoc(Document doc, URI docURI, Map<String, String> assetSubstitutes,
                                                    Properties generatorSettings) {
        var websiteBaseURI = URI.create(generatorSettings.getProperty("baseUrl"));
        var docBaseURI = determineDocBaseURI(docURI, doc);

        replaceStylesheetReferences(doc, websiteBaseURI, docBaseURI, assetSubstitutes);
        replaceJSReferences(doc, websiteBaseURI, docBaseURI, assetSubstitutes);
        replaceImageReferences(doc, websiteBaseURI, docBaseURI, assetSubstitutes);
    }

    /**
     * Replaces asset references in all HTML files found in supplied dir and sub dirs.
     */
    public static void replaceAssetsReferencesInDir(Path dir, Map<String, String> assetSubstitutes,
                                                    Properties generatorSettings) throws IOException {

        // Note: Right now, param assetSubstitutes holds asset refs of any kind.
        // If there's ever the need to speed things up, grouping assetSubstitutes
        // by asset type (i.e. image, js, css, etc.) could be an option.
        // But at the moment it's YAGNI.
        replaceAssetsReferencesInDirImpl(dir, dir, assetSubstitutes, generatorSettings);
    }

    /**
     * Implementation method for replacing asset references.
     *
     * The implementation method is necessary to not complicate the public API, which
     * shouldn't be required to supply the rootDir param (hint: rootDir needs be
     * dragged on through recursive executions.
     *
     * @param dir dir containing HTML files to process
     * @param rootDir root directory of the site needed to calculate
     *                the path of HTML files in order to properly resolve
     *                relative asset refs
     */
    private static void replaceAssetsReferencesInDirImpl(Path dir, Path rootDir, Map<String, String> assetSubstitutes,
                                                         Properties generatorSettings) throws IOException {
        // Replace references
        try (var htmlFiles = Files.newDirectoryStream(dir, FileFilters.htmlFilter)) {
            for (Path htmlFile : htmlFiles) {
                Document doc = Jsoup.parse(htmlFile.toFile(), "UTF-8");
                URI docURI = URI.create(rootDir.relativize(dir).resolve(htmlFile.getFileName()).toString());

                LOG.debug(String.format("Replacing asset references in '%s'", htmlFile));
                replaceAssetsReferencesInDoc(doc, docURI, assetSubstitutes, generatorSettings);

                Files.write(htmlFile, doc.outerHtml().getBytes());
            }
        }

        // Replace refs in sub directories
        try (var subDirs = FileFilters.subDirStream(dir)) {
            for (var subDir : subDirs) {
                replaceAssetsReferencesInDirImpl(subDir, rootDir, assetSubstitutes, generatorSettings);
            }
        }
    }

    private static void replaceStylesheetReferences(Document doc, URI websiteBaseURI, URI docBaseURI,
                                                    Map<String, String> stylesheetSubstitutes) {
        // Note: According to https://html.spec.whatwg.org/#interactions-of-styling-and-scripting,
        // `<link rel="stylesheet">` is the only way to load an external stylesheet.
        var elements = doc.select("link[rel='stylesheet']");
        replaceReferences(elements, "href", websiteBaseURI, docBaseURI, stylesheetSubstitutes);
    }

    private static void replaceJSReferences(Document doc, URI websiteBaseURI, URI docBaseURI,
                                            Map<String, String> substitutes) {
        // Note: Quick research didn't reveal any other method of loading an
        // external JavaScript file. And sites built with this generator
        // don't use any other way.
        var elements = doc.select("script[src]");
        replaceReferences(elements, "src", websiteBaseURI, docBaseURI, substitutes);
    }

    private static void replaceImageReferences(Document doc, URI websiteBaseURI, URI docBaseURI,
                                               Map<String, String> substitutes) {
        // Note: Only those ways to embed an image in HTML are supported that
        // are used right now by the sites built with C-3PO.

        // Replace refs in standard `src` attributes
        var elements = doc.select("img[src]");
        replaceReferences(elements, "src", websiteBaseURI, docBaseURI, substitutes);

        // Replace refs in `srcset` attributes
        // Note: It's special cause it can include multiple refs.
        // Therefore the generic method can't be used.
        for (Element element : elements) {
            if (element.hasAttr("srcset")) {
                var srcsetAttr = element.attr("srcset");

                var refs = HtmlSrcset.extractRefs(srcsetAttr);
                for (String ref : refs) {
                    var replacedRef = replaceReference(ref, websiteBaseURI, docBaseURI, substitutes);
                    srcsetAttr = srcsetAttr.replace(ref, replacedRef);
                }

                element.attr("srcset", srcsetAttr);
            }
        }
    }

    private static void replaceReferences(Elements elements, String refAttrName, URI websiteBaseURI, URI docBaseURI,
                                          Map<String, String> substitutes) {
        for (Element element : elements) {
            String assetRefValue = element.attr(refAttrName);
            element.attr(refAttrName, replaceReference(assetRefValue, websiteBaseURI, docBaseURI, substitutes));
        }
    }

    private static String replaceReference(String assetRefValue, URI websiteBaseURI, URI docBaseURI,
                                           Map<String, String> substitutes) {
        var assetURI = URI.create(assetRefValue);
        if (isAssetControlledByWebsite(assetURI, websiteBaseURI, docBaseURI)) {
            String assetPath = translateToAssetPath(assetURI, docBaseURI);
            String substitutePath = substitutes.get(assetPath);
            if (substitutePath != null) {

                // Note: Replace the asset's name only and leave the URL untouched otherwise.
                String oldAssetFileName = Paths.get(assetURI.getPath()).getFileName().toString();
                String newAssetFileName = Paths.get(substitutePath).getFileName().toString();

                // Ensure only last occurrence of file name is replaced since file
                // name can be part of the asset path as well, for example `css/main.css/main.css`.
                return Pattern.compile(oldAssetFileName + "$").matcher(assetRefValue).replaceFirst(newAssetFileName);
            } else {
                LOG.warn(String.format("Failed to substitute asset resource '%s'", assetRefValue));
            }
        }

        return assetRefValue;
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

    /**
     * Translates the given URI to its corresponding asset path.
     *
     * The passed URI can be an absolute URI, a protocol-relative URI, a root-relative URI,
     * or a relative URI. To get an overview of the various URI types, see
     * https://yodaconditions.net/blog/html-url-types.html.
     *
     * When a passed URI references the fingerprinted version of an asset,
     * this fingerprint is cut out from the result.
     *
     * @param assetRefURI the URI of the asset reference in HTML
     * @param docBaseURI the base URI of the document the asset is referenced in which is needed
     *                   to resolve document-relative URIs. Base URI means that the caller
     *                   also needs to consider a possible `<base>` element within the documents
     *                   markup.
     * @return the normalized asset path with a leading slash which represents
     *         the root directory of the project
     */
    private static String translateToAssetPath(URI assetRefURI, URI docBaseURI) {
        String assetPath;

        if (isDocumentRelativeURI(assetRefURI)) {
            assetPath = docBaseURI.resolve(assetRefURI).normalize().getPath();

            // Note: A leading slash is prepended, when Path.getPath() does not return a String with
            // a leading slash. This could be the case if the document's base URI does not have a path portion.
            // The leading slash is required to adhere to the pattern of a normalized asset path.
            assetPath = assetPath.startsWith("/") ? assetPath : "/" + assetPath;
        } else if (isProtocolRelativeURI(assetRefURI)) {
            assetPath = assetRefURI.normalize().getPath();
        } else if (isRootRelativeURI(assetRefURI)) {
            assetPath = assetRefURI.normalize().toString();
        } else if (assetRefURI.isAbsolute()) {
            assetPath = assetRefURI.normalize().getPath();
        } else {

            // Note: This could be an error as well, but in auto-build mode, there can
            // easy be a situation, that would raise and thus kill the process. One could
            // think about differentiating behavior based on one-time and auto-build mode
            // but there's not the necessary infrastructure (e.g. a generation context object)
            // for that right now.
            LOG.warn(String.format("Failed to discern URL type of '%s' when trying to map it to the corresponding " +
                    "site asset path. Leaving it unprocessed.", assetRefURI));
            assetPath = assetRefURI.toString();
        }

        return cutOptionalFingerprint(assetPath);
    }

    private static String cutOptionalFingerprint(String assetPath) {
        var matcher = FINGERPRINTED_ASSET_URI_PATTERN.matcher(assetPath);
        return matcher.matches() ? matcher.group(1) + matcher.group(2) : assetPath;
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
