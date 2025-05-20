package org.twelve.gcp.node.expression.referable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.node.expression.Expression;

import java.util.ArrayList;
import java.util.List;

public abstract class ReferAbleNode extends Expression {
    protected final List<ReferenceNode> refs = new ArrayList<>();
    public ReferAbleNode(AST ast, ReferenceNode... refs) {
        super(ast, null);
        for (ReferenceNode ref : refs) {
            this.refs.add(this.addNode(ref));
        }
    }

    public List<ReferenceNode> refs() {
        return this.refs;
    }
}
