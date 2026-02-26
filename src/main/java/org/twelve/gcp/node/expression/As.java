package org.twelve.gcp.node.expression;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

/**
 * let a =  b as {name:String}
 * expression as outline
 * huizi
 */
public class As extends Expression {
    private final Expression expression;
    private final TypeNode as;

    public As(Expression expression, TypeNode as) {
        super(expression.ast(),null);
        this.expression = this.addNode(expression);
        this.as = this.addNode(as);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public Expression expression() {
        return this.expression;
    }

    public TypeNode as(){
        return this.as;
    }

    @Override
    public String lexeme() {
        return expression.lexeme()+" as "+as.lexeme();
    }
}
