package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ADT;
import org.twelve.gcp.outline.builtin.Any_;

import static org.twelve.gcp.common.Tool.cast;

/**
 * literal type, a value type
 */
public class Literal extends Primitive {
    private final Outline origin;

    /** Accepts any expression node — not just ValueNode — so function/entity/tuple literals work. */
    public Literal(Node node, Outline outline, AST ast) {
        super(new Any_(), node, ast);
        this.origin = outline;
    }

    @Override
    public String toString() {
        return this.origin.toString();
    }

    @Override
    public boolean is(Outline another) {
        if (another instanceof Literal lit) {
            return this.origin.is(lit.origin) && nodesAreEqual(this.node(), lit.node());
        } else {
            return super.is(another);
        }
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another instanceof Literal) {
            return another.tryYouAreMe(this);
        } else {
            return this.origin.is(another);
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (!another.is(this.origin)) return false;
        return nodesAreEqual(this.node(), another.node());
    }

    /**
     * Compares two AST nodes for "same literal value" equality.
     * For {@link ValueNode} subtypes (string/number/entity/tuple) delegates to {@code isSame()};
     * for other node kinds (e.g. {@code FunctionNode}) falls back to reference equality,
     * which means a function literal type is only satisfied by the exact same AST node.
     */
    @SuppressWarnings("unchecked")
    private static boolean nodesAreEqual(Node a, Node b) {
        if (a instanceof ValueNode va && b instanceof ValueNode vb) {
            return va.isSame(vb);
        }
        return a == b;
    }

    public Outline outline() {
        return this.origin;
    }

    /**
     * In addition to the built-in methods registered by {@code ADT} (e.g. {@code to_str}),
     * also mirror all methods from the origin type (e.g. NUMBER methods on integer literals,
     * STRING methods on string literals) so that {@code 100.abs()} and {@code "hi".length()}
     * resolve correctly even though the host type is a Literal wrapper rather than the bare
     * primitive singleton.
     */
    @Override
    public boolean loadBuiltInMethods() {
        if (!super.loadBuiltInMethods()) return false;
        if (origin instanceof ADT originAdt) {
            originAdt.loadBuiltInMethods();
            originAdt.members().forEach(m -> members.putIfAbsent(m.name(), m));
        }
        return true;
    }
}
