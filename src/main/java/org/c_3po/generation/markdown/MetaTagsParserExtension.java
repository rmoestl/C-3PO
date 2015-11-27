package org.c_3po.generation.markdown;

import org.commonmark.parser.Parser;

/**
 * Parser extension that is responsible for parsing meta tag syntax.
 */
class MetaTagsParserExtension implements Parser.ParserExtension {

    private MetaTagsParserExtension() {
    }

    static MetaTagsParserExtension getInstance() {
        return new MetaTagsParserExtension();
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
         parserBuilder.customBlockParserFactory(new MetaTagsParser.Factory());
    }
}
