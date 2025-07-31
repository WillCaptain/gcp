package org.twelve.gcp.builder;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.statement.ExpressionStatement;
import org.twelve.gcp.node.statement.MemberNode;

import java.util.ArrayList;
import java.util.List;

public class TupleBuilder {
    private final AST ast;
    private List<Expression> members = new ArrayList<>();

    public TupleBuilder(AST ast) {
        this.ast = ast;
    }

    public TupleBuilder add(Token<?> token) {
        members.add(LiteralNode.parse(ast,token));
        return this;
    }
    public TupleBuilder add(Expression expression) {
        members.add(expression);
        return this;
    }
    public TupleNode build() {
        return new TupleNode(ast,members.toArray(new Expression[0]));
    }
}
