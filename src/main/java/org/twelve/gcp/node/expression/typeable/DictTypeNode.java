package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class DictTypeNode extends TypeNode{
    private final TypeNode keyNode;
    private final TypeNode valueNode;

    public DictTypeNode(AST ast, TypeNode keyNode, TypeNode valueNode) {
        super(ast);
        this.keyNode = this.addNode(keyNode);
        this.valueNode = this.addNode(valueNode);
    }
    public DictTypeNode(AST ast) {
        this(ast,null,null);
    }

    public TypeNode keyNode(){
        return this.keyNode;
    }
    public TypeNode valueNode(){
        return this.valueNode;
    }
    @Override
    public String lexeme() {
        if(this.keyNode==null) return "[?:?]";
        return "["+this.keyNode.lexeme()+" : "+this.valueNode.lexeme()+"]";
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
