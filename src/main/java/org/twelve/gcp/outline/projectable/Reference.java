package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.primitive.NOTHING;

import java.util.Map;

/**
 * 传统泛型
 */
public class Reference extends Genericable<Reference, ReferenceNode> implements Returnable{
    private Long argument;

    private Reference(ReferenceNode node, Outline declared) {
        super(node, declared);
    }
    private Reference(AST ast, Outline declared) {
        super(ast, declared);
    }

    public static Reference from(ReferenceNode node, Outline declared){
        return new Reference(node,declared);
    }
    public static Reference from(AST ast, Outline declared){
        return new Reference(ast,declared);
    }

    @Override
    protected Reference createNew() {
        if(this.node==null){
            return new Reference(this.ast(),this.declaredToBe);
        }else {
            return new Reference(this.node, this.declaredToBe);
        }
    }

    @Override
    public String toString() {
        return "<"+this.name()+">";
    }

    @Override
    public String name() {
        return this.node().name();
    }

    @Override
    public void setArgument(Long argument) {
        this.argument = argument;
    }

    @Override
    public Outline guess() {
//        return this.eventual();//todo
        return this;
    }

    @Override
    public Outline supposedToBe() {
        return this.ast().Nothing;
    }

    @Override
    public boolean addReturn(Outline ret) {
        return false;
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
//        Reference me = this;
//        Optional<Pair<Reference,Outline>> you = Arrays.stream(projections).filter(p->p.key().id()==me.id()).findFirst();
        if(reference.id()==this.id){
            return this.project(this, projection.outline(), new ProjectSession());

        }else{
            return this;
        }
    }

    @Override
    public Outline eventual() {
        if (this.extendToBe() instanceof NOTHING) {
            GCPErrorReporter.report(this.node, GCPErrCode.NOT_INITIALIZED);
        }
        return this.extendToBe().eventual();
    }

    @Override
    public Reference copy() {
        Reference copied = super.copy();
        copied.argument = this.argument;//.copy();
        return copied;
    }

    @Override
    public Reference copy(Map<Outline, Outline> cache) {
        Reference copied = super.copy(cache);
        copied.argument = this.argument;//.copy(cache);
        return copied;
    }
}
