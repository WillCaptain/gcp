package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.UnpackAble;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

/**
 * a is b as c
 */
public class IsAs extends Expression {
    private final Expression a;
    private final UnpackAble c;
    private final TypeNode b;

    public IsAs(Expression a, TypeNode b, UnpackAble c) {
        super(a.ast(), null);
        this.a = this.addNode(a);
        this.b = this.addNode(b);
        this.c = c;
        if (a != c) this.addNode(c);
    }

    public IsAs(Identifier a, TypeNode is) {
        this(a, is, a);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public Expression a() {
        return a;
    }

    public Identifier c() {
        return cast(c);
    }

    public TypeNode b() {
        return b;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        sb.append(a).append(" is ").append(b);
        if (a != c) {
            sb.append(" as ").append(c);
        }
        return sb.toString();
    }
}
