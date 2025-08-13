package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Integer_;

public class INTEGER extends LONG {
    private final static Integer_ int_ = new Integer_();

    public INTEGER(Node node) {
        super(int_, node, node.ast());
       this.loadMethods();
    }

    public INTEGER(AST ast) {
        super(int_, null, ast);
    }

}
