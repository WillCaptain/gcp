package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

/**
 * a is b as c
 */
public class IsAs extends Expression {
    private final Expression a;
    private final Identifier c;
    private final Outline b;

    public IsAs(AST ast, Expression a, Outline b, Identifier c) {
        super(ast, null);
        this.a = a;
        this.b = b;
        this.c = c;
        this.addNode(a);
        if (a != c) this.addNode(c);
    }

    public IsAs(AST ast, Identifier a, Outline is) {
        this(ast, a, is, a);
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public Expression a() {
        return a;
    }

    public Identifier c() {
        return c;
    }

    public Outline b() {
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
