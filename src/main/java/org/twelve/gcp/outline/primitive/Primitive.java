package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.*;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

public abstract class Primitive extends ProductADT {

//    private boolean created = false;
    private Node node;

    protected Primitive(BuildInOutline buildIn, Node node, AST ast) {
        super(ast, buildIn);
        this.node = node;
//        this.created = true;
    }

    @Override
    public long id() {
        return buildIn.id();
    }

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        return this.getClass().isInstance(another);
    }

    @Override
    public boolean is(Outline another) {
        // SYMBOL types are unresolved symbolic references; allow name-based matching so that
        // SYMBOL("Bool").is(BOOL) and BOOL.is(SYMBOL("Bool")) both return true.
        if (another instanceof SYMBOL && this.toString().equals(another.toString())) return true;
        return super.is(another);
    }
}
