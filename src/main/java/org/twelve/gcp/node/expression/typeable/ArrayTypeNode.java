package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;

public class ArrayTypeNode extends TypeNode{
    private final TypeNode itemNode;

    public ArrayTypeNode(AST ast, TypeNode itemNode) {
        super(ast);
        this.itemNode = this.addNode(itemNode);
    }
    public ArrayTypeNode(AST ast) {
        this(ast,null);
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
