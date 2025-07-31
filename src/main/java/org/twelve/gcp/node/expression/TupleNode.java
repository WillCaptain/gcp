package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TupleNode extends EntityNode {
    private static List<MemberNode> createMembers(Expression[] values) {
        List<MemberNode> memberNodes = new ArrayList<>();
        for (Integer i=0; i<values.length; i++) {
            Expression value = values[i];
           memberNodes.add(new MemberNode(new Identifier(value.ast(),
                    new Token<>(i.toString())),value,false));
        }
        return memberNodes;
    }

    private final Expression[] values;

    public TupleNode(AST ast, Expression... values) {
        super(createMembers(values));
        this.values = values;
    }
    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder("(");
        sb.append(Arrays.stream(values).map(Node::lexeme).collect(Collectors.joining(",")))
                .append(")");
        return sb.toString();
    }
}
