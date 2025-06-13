package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.builtin.Array_;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;
import org.twelve.gcp.outline.projectable.Reference;

import static org.twelve.gcp.common.Tool.cast;

public class Array extends ProductADT implements Projectable {//} implements GenericContainer {
    private final Node node;
    private final Outline itemOutline;

    public Array(Node node, Outline itemOutline) {
        super(Array_.instance());
        this.node = node;
        this.itemOutline = itemOutline;
    }

    public Outline itemOutline() {
        return this.itemOutline;
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public String toString() {
        return "[" + itemOutline + "]";
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another instanceof Array) {
            return this.itemOutline.is(((Array) another).itemOutline);
        } else {
            return false;
        }
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        /*Outline projection = this.itemOutline;
        for (Pair<Reference, Outline> p : projections) {
            if (p.key().id() == this.itemOutline.id()) {
                projection = p.value();
                break;
            }
        }*/
        if (reference.id() == this.itemOutline.id()) {
            return new Array(this.node, projection.outline());
        } else {
            return this;
        }
    }

    @Override
    public boolean containsUnknown() {
        return this.itemOutline.containsUnknown();
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (!projection.is(this)) {
            ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
            return this.guess();
        }
        Array you = cast(projection);
        if (this.itemOutline instanceof Projectable) {
            ((Projectable) this.itemOutline).project(cast(this.itemOutline), you.itemOutline, session);
        }
        return projection;
    }

    @Override
    public Outline guess() {
        if (this.itemOutline instanceof Projectable) {
            return new Array(this.node, ((Projectable) this.itemOutline).guess());
        } else {
            return this;
        }
    }

//    @Override
//    public void addHasToBe(Outline hasToBe) {
//        if(!(hasToBe instanceof Array)) return;
//        if(this.itemOutline instanceof Genericable<?,?>){
//            ((Genericable<?, ?>) this.itemOutline).addHasToBe(((Array) hasToBe).itemOutline);
//        }
//    }
}
