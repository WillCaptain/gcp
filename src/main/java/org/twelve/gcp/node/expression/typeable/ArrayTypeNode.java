package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;

public class ArrayTypeNode extends TypeNode{
    private final TypeNode itemNode;

    public ArrayTypeNode(AST ast, TypeNode itemNode) {
        super(ast);
        if(itemNode instanceof Question){
            this.itemNode = null;
        }else {
            this.itemNode = this.addNode(itemNode);
        }
    }
    public ArrayTypeNode(AST ast) {
        this(ast,null);
    }

    public TypeNode itemNode(){
        return this.itemNode;
    }
    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    @Override
    public String lexeme() {
        if(this.itemNode==null){
            return "[?]";
        }else{
            return "["+this.itemNode.lexeme()+"]";
        }
    }
}
