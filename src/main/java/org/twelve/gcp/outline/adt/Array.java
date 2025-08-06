package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.builtin.ANY;
import org.twelve.gcp.outline.builtin.Array_;
import org.twelve.gcp.outline.builtin.NOTHING;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class Array extends DictOrArray<INTEGER> {//} implements GenericContainer {

    public Array(Node node, Outline itemOutline) {
        super(node,Array_.instance(),Outline.Integer,itemOutline);
    }

    public Outline itemOutline() {
        return this.value;
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public String toString() {
        return "[" + value + "]";
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        if (reference.id() == this.value.id()) {
            return new Array(this.node, projection.outline());
        } else {
            return this;
        }
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if(projected.id()==this.id()) {
            if (!projection.is(this)) {
                ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
                return this.guess();
            }
            Array you = cast(projection);
            if (this.value instanceof Projectable) {
                ((Projectable) this.value).project(cast(this.value), you.value, session);
            }
            return projection;
        }else{
            if(this.value instanceof Projectable){
                return new Array(this.node,((Projectable) this.value).project(projected,projection,session));
            }else{
                return this;
            }
        }
    }

    @Override
    public Outline guess() {
        if (this.value instanceof Projectable) {
            return new Array(this.node, ((Projectable) this.value).guess());
        } else {
            return this;
        }
    }

    @Override
    public Array copy(Map<Long, Outline> cache) {
        Array copied = cast(cache.get(this.id()));
        if(copied==null){
            copied = new Array(this.node,this.value.copy(cache));
            cache.put(this.id(),copied);
        }
        return copied;
    }

    @Override
    public boolean beAssignable() {
        return this.value.beAssignable();
    }
    @Override
    public Array alternative(){
        if(this.value instanceof NOTHING){
            return new Array(this.node, Any);
        }else{
            return this;
        }
    }
}
