package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionTypeNode extends TypeNode {
    private final List<TypeNode> arguments=new ArrayList<>();
    private final TypeNode returns;

    public FunctionTypeNode(AST ast, TypeNode returns, TypeNode... args) {
        super(ast);
        for (TypeNode arg : args) {
            this.arguments.add(this.addNode(arg));
        }
        this.returns = this.addNode(returns);
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        return new StringBuilder()
                .append(this.arguments.stream().map(Node::lexeme).collect(Collectors.joining("->")))
                .append("->"+this.returns.lexeme()).toString();
    }

    public TypeNode returns() {
        return returns;
    }

    public List<TypeNode> arguments() {
        return arguments;
    }
}
