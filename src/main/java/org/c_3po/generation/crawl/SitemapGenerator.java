package org.c_3po.generation.crawl;

import org.c_3po.generation.GenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for turning an instance of {@link SiteStructure} into a XML sitemap
 * according to <a href="http://www.sitemaps.org/">http://www.sitemaps.org/</a>.
 */
public class SitemapGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SitemapGenerator.class);
    private static final String NAMESPACE_URI = "http://www.sitemaps.org/schemas/sitemap/0.9";
    private static final String ELEM_URLSET = "urlset";

    // Make it non-instantiable and prohibit subclassing.
    private SitemapGenerator() {
        throw new AssertionError();
    }

    /**
     * Generates a sitemap file compliant to the sitemap XML standard
     * defined at <a href="http://www.sitemaps.org/protocol.html">http://www.sitemaps.org/protocol.html</a>.
     *
     * @param siteStructure the SiteStructure that is to be written to a sitemap xml file
     * @param filePath the path to the <strong>file</strong> to where the sitemap should be written to
     */
    public static void generate(SiteStructure siteStructure, Path filePath) throws GenerationException {
        Objects.requireNonNull(siteStructure, "siteStructure must not be null");
        Objects.requireNonNull(filePath, "filePath must not be null");
        List<String> urls = siteStructure.toUrls();

        try {
            Document document = newSitemapDocument();
            Element urlSetElem = document.getDocumentElement();
            urls.forEach(url -> urlSetElem.appendChild(newUrlElem(url, document)));

            writeToFile(document, filePath);
        } catch (ParserConfigurationException | TransformerException e) {
            LOG.debug("Failed to generate sitemap.xml. See enclosed exception for more details.", e);
            throw new GenerationException("Failed to generate sitemap xml file", e);
        }
    }

    private static Document newSitemapDocument() throws ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.getDOMImplementation().createDocument(NAMESPACE_URI, ELEM_URLSET, null);
    }

    private static Element newUrlElem(String url, Document document) {
        Element urlElem = document.createElement("url");
        urlElem.appendChild(newLocElem(url, document));
        return urlElem;
    }

    private static Element newLocElem(String url, Document document) {
        Element locElem = document.createElement("loc");
        locElem.setTextContent(url);
        return locElem;
    }

    private static void writeToFile(Document document, Path filePath) throws TransformerException {

        // Note: for XML UTF-8 is the default character encoding. Therefore we assume that
        // Transformer uses UTF-8 as well.
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(filePath.toFile()));
    }
}
