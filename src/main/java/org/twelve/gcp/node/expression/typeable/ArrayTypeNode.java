package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class ArrayTypeNode extends TypeNode{
    private final TypeNode itemNode;

    public ArrayTypeNode(AST ast, TypeNode itemNode) {
        super(ast);
        this.itemNode = this.addNode(itemNode);
    }
    public ArrayTypeNode(AST ast) {
        this(ast,null);
    }

    public TypeNode itemNode(){
        return this.itemNode;
    }
    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        if(this.itemNode==null){
            return "[]";
        }else{
            return "["+this.itemNode.lexeme()+"]";
        }
    }
}
