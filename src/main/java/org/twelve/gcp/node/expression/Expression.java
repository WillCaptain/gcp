package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;

public abstract class Expression extends Node {
    public Expression(AST ast, Location loc) {
        super(ast, loc);
    }
}
