package org.twelve.gcp.builder;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.node.expression.DictNode;
import org.twelve.gcp.node.expression.Expression;

import java.util.ArrayList;
import java.util.List;

public class DictNodeBuilder {
    private final AST ast;
    private final List<Pair<Expression, Expression>> values = new ArrayList<>();

    public DictNodeBuilder(AST ast) {
        this.ast = ast;
    }

    public DictNodeBuilder add(Expression key, Expression value) {
        this.values.add(new Pair<>(key,value));
        return this;
    }
    public DictNode build(){
        return new DictNode(ast,values.toArray(new Pair[0]));
    }
}
