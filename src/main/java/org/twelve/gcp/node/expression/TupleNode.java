package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

public class TupleNode extends EntityNode {
    private static List<MemberNode> createMembers(Expression[] values) {
        List<MemberNode> memberNodes = new ArrayList<>();
        for (Integer i = 0; i < values.length; i++) {
            Expression value = values[i];
            memberNodes.add(new MemberNode(new Identifier(value.ast(),
                    new Token<>(i.toString())), value, false));
        }
        return memberNodes;
    }

    private final Expression[] values;

    public TupleNode(Node base, Expression... values) {
        super(createMembers(values), base);
        this.values = values;
    }
    public TupleNode(Expression... values) {
        super(createMembers(values));
        this.values = values;
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder((base == null ? "" : base.lexeme()) + "(");
        sb.append(Arrays.stream(values).map(AbstractNode::lexeme).collect(Collectors.joining(",")))
                .append(")");
        return sb.toString();
    }
}
