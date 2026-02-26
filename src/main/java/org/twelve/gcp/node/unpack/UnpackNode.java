package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;

import java.util.List;

public abstract class UnpackNode extends Assignable {
    public UnpackNode(AST ast, Location loc) {
        super(ast, loc);
//        this.outline = new Unpack(this);
    }
    @Override
    public String lexeme() {
        return this.toString();
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    abstract public List<Identifier> identifiers();
}
