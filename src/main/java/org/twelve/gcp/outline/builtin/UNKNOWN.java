package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.Primitive;

/**
 * outline before type inference
 */
public class UNKNOWN extends BuildInOutline {
    private final AbstractNode node;

    public UNKNOWN(AbstractNode node) {
        super(node.ast());
        this.node = node;
    }

    public UNKNOWN(AST ast) {
        super(ast);
        this.node = null;
    }
    @Override
    public AbstractNode node() {
        return this.node;
    }

    @Override
    public String toString() {
        return "?";
    }

    @Override
    public boolean beAssignedAble() {
        return false;
    }

    @Override
    public boolean containsUnknown() {
        return true;
    }

    @Override
    public boolean beAssignable() {
        return false;
    }

    @Override
    public long id() {
        return CONSTANTS.UNKNOWN_INDEX;
    }
    @Override
    public boolean tryYouAreMe(Outline another) {
        return true;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        return false;
    }
}
