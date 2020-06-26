package org.c_3po.generation.assets;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Represents the HTML `srcset` attribute.
 */
final class HtmlSrcset {
    /**
     * Extracts refs (URLs) from the passed `srcset` attribute which
     * means it cuts out source descriptions and such stuff.
     *
     * <b>Caution</b>: It does not fully implement the standard. If an URL
     * contains a comma `,`, this function does not work.
     *
     * @param srcsetAttr the value of a srcset attribute
     * @return a list of refs (URLs) contained in the passed srcset attribute
     */
    static List<String> extractRefs(String srcsetAttr) {
        var sources = List.of(srcsetAttr.split(","));
        return sources.stream()
                .map(source -> {
                    var trimmedSource = source.trim();
                    return trimmedSource.split("\\s")[0];
                }).collect(toList());
    }
}
