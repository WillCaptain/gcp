package org.twelve.gcp.node.expression.referable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReferenceCallNode extends Expression {
    private final List<TypeNode> types;
    private final Expression host;

    public ReferenceCallNode(AST ast, Expression host, TypeNode... types) {
        super(ast, null);
        this.host = host;
        this.addNode(host);
        this.types = Arrays.asList(types);
        for (TypeNode type : types) {
            this.addNode(type);
        }
    }

    public List<TypeNode> types() {
        return this.types;
    }

    public Expression host() {
        return this.host;
    }

    @Override
    public String lexeme() {
        return new StringBuilder(this.host.lexeme())
                .append("<")
                .append(this.types.stream().map(Node::lexeme).collect(Collectors.joining(",")))
                .append(">").toString();
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
