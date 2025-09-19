package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

import static org.twelve.gcp.common.Tool.cast;

public class DictOrArray<K extends Outline> extends ProductADT implements Projectable {//} implements GenericContainer {
    protected final AbstractNode node;
    protected final K key;
    protected final Outline value;

    public DictOrArray(AbstractNode node, AST ast, BuildInOutline base, K key, Outline value) {
        super(ast,base);
        this.node = node;
        this.key = key;
        this.value = value;
    }

    @Override
    public AbstractNode node() {
        return this.node;
    }

    @Override
    public String toString() {
        return "[..]";
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another.getClass().isInstance(this)) {
            DictOrArray<?> you = cast(another);
            return this.value.is(you.value) && this.key.is(you.key);
        } else {
            return false;
        }
    }

    @Override
    public boolean maybe(Outline another) {
        return true;
    }

    @Override
    public boolean containsUnknown() {
        return this.value.containsUnknown() || this.key.containsUnknown();
    }


    @Override
    public boolean emptyConstraint() {
        return this.value instanceof Projectable && ((Projectable) this.value).emptyConstraint() &&
                this.key instanceof Projectable && ((Projectable) this.key).emptyConstraint();
    }

    @Override
    public boolean containsGeneric() {
        return this.value instanceof Projectable && ((Projectable) this.value).containsGeneric() &&
                this.key instanceof Projectable && ((Projectable) this.key).containsGeneric();
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (projected.id() == this.id()) {
            if (!projection.is(this)) {
                GCPErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
                return this.guess();
            }
            DictOrArray<?> you = cast(projection);
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
        return this;
    }
}
