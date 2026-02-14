package org.twelve.gcp.node.expression.body;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;

public class WithExpression extends Block{
    private final Expression resource;
    private final Identifier as;
    private final Block body;

    public WithExpression(AST ast, Expression resource, Identifier as,Block body) {
        super(ast);
        this.resource = this.addNode(resource);
        this.as = this.addNode(as);
        this.body = this.addNode(body);
    }

    public Expression resource(){
        return this.resource;
    }

    public Identifier as(){
        return this.as;
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        return "with "+ resource().lexeme()+(as()==null?"":(" as "+as().lexeme()))+ body().lexeme();
    }

    public Block body() {
        return this.body;
    }
}
