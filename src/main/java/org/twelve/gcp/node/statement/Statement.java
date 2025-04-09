package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.ONode;

public abstract class Statement extends ONode {
    public Statement(OAST ast) {
        super(ast);
    }
}
