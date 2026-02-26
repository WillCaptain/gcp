package org.twelve.gcp.node.expression.referable;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

public class ReferenceCallNode extends Expression {
    private final List<TypeNode> types;
    private final Expression host;

    public ReferenceCallNode(Expression host, TypeNode... types) {
        super(host.ast(), null);
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
                .append(this.types.stream().map(AbstractNode::lexeme).collect(Collectors.joining(",")))
                .append(">").toString();
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }

}
