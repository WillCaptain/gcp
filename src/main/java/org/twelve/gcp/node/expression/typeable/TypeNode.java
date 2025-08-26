package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Node;

public abstract class TypeNode extends Node {
    public TypeNode(AST ast) {
        super(ast);
    }
    public TypeNode(AST ast, Location loc) {
        super(ast,loc);
    }
//    @Override
//    public Outline infer(Inferences inferences) {
//        return new OutlineWrapper(this, super.infer(inferences));
//    }
}
