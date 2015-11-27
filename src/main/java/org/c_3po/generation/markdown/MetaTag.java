package org.c_3po.generation.markdown;

import org.commonmark.node.CustomBlock;
import org.commonmark.node.Visitor;

/**
 * A AST block node representing a meta tag.
 */
public class MetaTag extends CustomBlock {
    private final String name;
    private final String content;

    MetaTag(String name, String content) {
        this.name = name;
        this.content = content;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
