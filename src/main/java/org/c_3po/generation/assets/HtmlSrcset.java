package org.c_3po.generation.assets;

import java.util.List;

/**
 * Represents the HTML `srcset` attribute.
 */
final class HtmlSrcset {
    static List<String> parse(String srcsetAttr) {
        // TODO: Document that is not fully compatible with standard because an URL could have commas in it as well
        return List.of(srcsetAttr.split(","));
    }
}
