package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AbstractNode;

public abstract class TypeNode extends AbstractNode {
    public TypeNode(AST ast) {
        super(ast);
    }
    public TypeNode(AST ast, Location loc) {
        super(ast,loc);
    }

    @Override
    public boolean inferred() {
        return this.outline.inferred();
    }
}
