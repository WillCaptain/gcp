package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
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
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
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
