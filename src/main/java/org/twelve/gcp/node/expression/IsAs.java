package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.UnpackAble;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

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
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
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
