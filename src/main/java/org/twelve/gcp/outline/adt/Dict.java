package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.builtin.Dict_;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class Dict extends DictOrArray<Outline> {
    public Dict(AbstractNode node, Outline key, Outline value) {
        super(node,node.ast(), Dict_.instance(), key, value);
    }
    public Dict(AST ast, Outline key, Outline value) {
        super(null,ast, Dict_.instance(), key, value);
    }

    public Outline key(){
        return this.key;
    }

    public Outline value(){
        return this.value;
    }

    @Override
    public String toString() {
        return "[" + key + " : " + value + "]";
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        if (reference.id() == this.key.id()) {
            return new Dict(this.node, projection.outline(), this.value);
        }
        if (reference.id() == this.value.id()) {
            return new Dict(this.node, this.key, projection.outline());
        }
        return this;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (projected.id() == this.id()) {
            if (!projection.is(this)) {
                GCPErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
                return this.guess();
            }
            Dict you = cast(projection);
            if (this.key instanceof Projectable) {
                ((Projectable) this.key).project(cast(this.key), you.key, session);
            }
            if (this.value instanceof Projectable) {
                ((Projectable) this.value).project(cast(this.value), you.value, session);
            }
            return projection;
        } else {
            if (!(this.key instanceof Projectable) && !(this.value instanceof Projectable)) return this;
            Outline newKey = this.key;
            Outline newValue = this.value;
            if (this.key instanceof Projectable) {
                newKey = ((Projectable) this.key).project(projected, projection, session);
            }
            if (this.value instanceof Projectable) {
                newValue = ((Projectable) this.value).project(projected, projection, session);
            }
            return new Dict(this.node, newKey, newValue);
        }
    }

    @Override
    public Outline guess() {
        if (!(this.key instanceof Projectable) && !(this.value instanceof Projectable)) return this;
        Outline newKey = this.key;
        Outline newValue = this.value;

        if (this.key instanceof Projectable) {
            newKey = ((Projectable) this.key).guess();
        }
        if (this.value instanceof Projectable) {
            newValue = ((Projectable) this.value).guess();
        }
        return new Dict(this.node, newKey, newValue);
    }

    @Override
    public Dict copy(Map<Outline, Outline> cache) {
        Dict copied = cast(cache.get(this));
        if (copied == null) {
            copied = new Dict(this.node, this.key.copy(cache), this.value.copy(cache));
            cache.put(this, copied);
        }
        return copied;
    }

    @Override
    public boolean beAssignable() {
        return this.key.beAssignable() && this.value.beAssignable();
    }

    @Override
    public Dict alternative() {
        if (this.key instanceof NOTHING) {
            return new Dict(this.node, this.node.ast().Any,this.node.ast().Any);
        } else {
            return this;
        }
    }

}
