package org.c_3po.generation.markdown;

import org.commonmark.node.Block;
import org.commonmark.parser.block.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser implementation of custom markdown extension that allows to specify meta tags.
 */
class MetaTagsParser extends AbstractBlockParser {
    private static final Pattern META_PATTERN = Pattern.compile("^\\$meta-([a-zA-Z\\-_\\. ]*):(.*)");

    private final MetaTag metaTag;

    private MetaTagsParser(MetaTag metaTag) {
        this.metaTag = metaTag;
    }

    @Override
    public Block getBlock() {
        return metaTag;
    }

    @Override
    public BlockContinue tryContinue(ParserState parserState) {
        // Note: it might be useful to have a meta description tag that spans multiple lines, but
        // to keep it simple we don't support that by now
        return BlockContinue.none();
    }

    public static class Factory extends AbstractBlockParserFactory {

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            if (state.getIndex() > 0) {
                return BlockStart.none();
            }

            CharSequence line = state.getLine();
            Matcher matcher;
            if ((matcher = META_PATTERN.matcher(line)).find()) {
                String name = matcher.group(1);
                String content = matcher.group(2).trim();
                MetaTag metaTag = new MetaTag(name, content);
                return BlockStart.of(new MetaTagsParser(metaTag)).atIndex(line.length());
            } else {
                return BlockStart.none();
            }
        }
    }
}
