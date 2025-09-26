package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Bool_;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

public class BOOL extends Primitive {
    private final static Bool_ bool_ = new Bool_();

    public BOOL(AbstractNode node) {
        super(bool_, node, node.ast());
        this.loadMethods();
    }

    public BOOL(AST ast) {
        super(bool_, null, ast);
    }

}
