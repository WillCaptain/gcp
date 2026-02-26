package org.twelve.gcp.node.expression.referable;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

/**
 * traditional generic type T in <T>
 */
public class ReferenceNode extends Identifier {
    private final TypeNode declared;
    public ReferenceNode(Identifier ref, TypeNode declared) {
        super(ref.ast(), ref.token());
        this.declared = this.addNode(declared);

    }

    @Override
    public String lexeme() {
        if(declared==null){
            return super.lexeme();
        }else{
            return super.lexeme()+":"+declared.lexeme();
        }
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    public TypeNode declared(){
        return declared;
    }
}
