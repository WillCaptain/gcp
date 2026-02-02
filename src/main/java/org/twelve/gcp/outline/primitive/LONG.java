package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Long_;

public class LONG extends FLOAT {
    private final static Long_ long_ = new Long_();

    protected LONG(BuildInOutline buildInOutline, Node node, AST ast) {
        super(buildInOutline, node, ast);
    }

    public LONG(Node node) {
        this(long_, node, node.ast());
        this.loadBuiltInMethods();
    }

    public LONG(AST ast) {
        this(long_, null, ast);
    }
}
