package org.twelve.gcp.node.expression.body;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

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
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    @Override
    public String lexeme() {
        return "with "+ resource().lexeme()+(as()==null?"":(" as "+as().lexeme()))+ body().lexeme();
    }

    public Block body() {
        return this.body;
    }
}
