package org.twelve.gcp.outline.decorators;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public record OutlineWrapper(AbstractNode node, Outline outline) implements Outline {

    @Override
    public AST ast() {
        return this.node().ast();
    }

    @Override
    public long id() {
        return CONSTANTS.OUTLINE_WRAPPER;
    }

    @Override
    public String toString() {
        return outline.toString();
    }
}
