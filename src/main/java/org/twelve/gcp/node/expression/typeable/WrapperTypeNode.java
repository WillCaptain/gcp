package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class WrapperTypeNode extends TypeNode {
    private final Location loc;

    public WrapperTypeNode(AST ast, Outline outline,Location loc){
        super(ast);
        this.outline = outline;
        this.loc = loc;
    }
    public WrapperTypeNode(AST ast, Outline outline){
        this(ast,outline,null);
    }

    @Override
    public Outline accept(Inferences inferences) {
        return this.outline;
    }

    @Override
    public Outline outline() {
        return outline;
    }

    @Override
    public Location loc() {
        return loc;
    }

    @Override
    public String lexeme() {
        return outline.toString();
    }
}
