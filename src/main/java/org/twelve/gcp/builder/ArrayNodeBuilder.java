package org.twelve.gcp.builder;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.ArrayNode;
import org.twelve.gcp.node.expression.Expression;

import java.util.ArrayList;
import java.util.List;

public class ArrayNodeBuilder  {
    private final AST ast;
    private final List<Expression> values = new ArrayList<>();

    public ArrayNodeBuilder(AST ast) {
        this.ast = ast;
    }

    public ArrayNodeBuilder add(Expression value) {
        this.values.add(value);
        return this;
    }
    public ArrayNode build(){
        return new ArrayNode(ast,values.toArray(new Expression[0]));
    }
}
