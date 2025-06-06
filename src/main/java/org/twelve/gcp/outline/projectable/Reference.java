package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.common.Pair;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.NOTHING;

import java.util.Arrays;
import java.util.Optional;

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
    public Outline project(Pair<Reference,Outline>[] projections) {
        Reference me = this;
        Optional<Pair<Reference,Outline>> you = Arrays.stream(projections).filter(p->p.key().id()==me.id()).findFirst();
        if(you.isPresent()){
            return this.project(this, you.get().value(), new ProjectSession());

        }else{
            return this;
        }
    }

    @Override
    public Outline eventual() {
        if (this.extendToBe() instanceof NOTHING) {
            ErrorReporter.report(this.node, GCPErrCode.NOT_INITIALIZED);
        }
        return this.extendToBe().eventual();
    }
}
