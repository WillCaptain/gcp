package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class ExtendTypeNode extends TypeNode{
    private final TypeNode base;
    private final EntityTypeNode extension;

    public ExtendTypeNode(AST ast, TypeNode base, EntityTypeNode extension) {
        super(ast);
        this.base = this.addNode(base);
        this.extension = this.addNode(extension);
    }

    public TypeNode base(){
        return this.base;
    }

    public EntityTypeNode extension(){
        return this.extension;
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
