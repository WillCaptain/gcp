package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.ONode;

public abstract class Expression extends ONode {
    public Expression(OAST ast, Location loc) {
        super(ast, loc);
    }
}
