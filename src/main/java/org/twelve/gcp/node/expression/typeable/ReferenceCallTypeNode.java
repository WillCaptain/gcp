package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReferenceCallTypeNode extends TypeNode {
    private final List<TypeNode> typeNodes = new ArrayList<>();
    private final TypeNode host;

    public ReferenceCallTypeNode(AST ast, TypeNode host, List<TypeNode> types) {
        super(ast);
        this.host = this.addNode(host);
        for (TypeNode type : types) {
            this.typeNodes.add(this.addNode(type));
        }
    }

    public List<TypeNode> typeNodes(){
        return this.typeNodes;
    }

    public TypeNode host(){
        return this.host;
    }

    @Override
    public String lexeme() {
        return host.lexeme()+"<"+typeNodes.stream().map(AbstractNode::lexeme).collect(Collectors.joining(","))+">";
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
