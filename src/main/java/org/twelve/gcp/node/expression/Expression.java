package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;

public abstract class Expression extends AbstractNode {
    public Expression(AST ast, Location loc) {
        super(ast, loc);
    }
}
