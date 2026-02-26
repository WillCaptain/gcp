package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;

public class IdentifierTypeNode extends TypeNode {
    private Identifier identifier;

    public IdentifierTypeNode(Identifier identifier) {
        super(identifier.ast());
        this.identifier = identifier;
    }
    public String name(){
        if(this.identifier.name().equals("?")) return "Unknown";
        return this.identifier.name();
    }
    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    @Override
    public Location loc() {
        return this.identifier.loc();
    }

    @Override
    public String lexeme() {
        return this.identifier.name();
    }

}
