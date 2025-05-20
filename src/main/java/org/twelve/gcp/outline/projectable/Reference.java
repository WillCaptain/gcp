package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.NOTHING;

import static org.twelve.gcp.common.Tool.cast;

/**
 * 传统泛型
 */
public class Reference extends Genericable<Reference, ReferenceNode> {
    public Reference(ReferenceNode node, Outline declared) {
        super(node, declared);
    }

    @Override
    protected Reference createNew() {
        return new Reference(cast(this.node), this.declaredToBe);
    }

//    @Override
//    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
//        if (projected != this) return this;//not me
//        if (this.projected != null) {
//            if (projection.is(this.projected)) {
//                return this.projected;
//            } else {
//                return this;
//            }
//        }
//        if (projection.is(this.declaredToBe)) {
//            this.projected = projection;
//            return this.projected;
//        } else {
//            return this;
//        }
//    }

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public String name() {
        return this.node().name();
    }

    @Override
    public Outline guess() {
        return this.eventual();//todo
    }

    @Override
    public Outline project(Reference me, Outline you) {
        if (this.id() != me.id()) return this;
        return this.project(this, you, new ProjectSession());
    }

    @Override
    public Outline eventual() {
        if (this.extendToBe() instanceof NOTHING) {
            ErrorReporter.report(this.node, GCPErrCode.NOT_INITIALIZED);
        }
        return this.extendToBe().eventual();
    }
}
