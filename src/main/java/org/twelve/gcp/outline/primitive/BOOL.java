package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.outline.builtin.Bool_;

public class BOOL extends Primitive {
    private final static Bool_ bool_ = new Bool_();

    public BOOL(AbstractNode node) {
        super(bool_, node, node.ast());
        this.loadBuiltInMethods();
    }

    public BOOL(AST ast) {
        super(bool_, null, ast);
    }

}
