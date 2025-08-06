package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.*;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Primitive extends ProductADT {

    private boolean created = false;
    private Node node;

    protected Primitive(BuildInOutline buildIn) {
        this(buildIn,null);
    }

    protected Primitive(BuildInOutline buildIn, Node node) {
        super(buildIn);
        this.node = node;
        this.created = true;
    }

    /**
     * post bind primitive methods
     */
    public static void initialize() {
        Outline.String.init();
        Outline.Decimal.init();
        Outline.Double.init();
        Outline.Float.init();
        Outline.Integer.init();
        Outline.Long.init();
        Outline.Boolean.init();
        Outline.Number.init();
    }

    @Override
    protected final void init() {
       if(this.created) {
           super.init();
       }
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
}
