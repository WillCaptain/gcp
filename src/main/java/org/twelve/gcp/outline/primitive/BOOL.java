package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.Bool_;

public class BOOL extends Primitive {
    private final static Bool_ bool_ = new Bool_();

    public BOOL(Node node) {
        super(bool_, node, node.ast());
        this.loadMethods();
    }

    public BOOL(AST ast) {
        super(bool_, null, ast);
    }
}
