package org.twelve.gcp.outline.decorators;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;
import org.twelve.gcp.outline.projectable.ReferAble;
import org.twelve.gcp.outline.projectable.Reference;
import org.twelve.gcp.outlineenv.AstScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Lazy implements Projectable, ReferAble {
    private final long id;
    private final Node node;
    private final Inferencer inferencer;
    private final AstScope scope;
    private List<OutlineWrapper> referencesProjections = new ArrayList<>();
    private Map<ProjectSession, GenericProjection> genericProjections = new HashMap<>();
//    private ProductADT me;

    public Lazy(Node node, Inferencer inferencer) {
        this.id = node.ast().Counter.getAndIncrement();
        this.node = node;
        this.scope = node.ast().symbolEnv().current();
        this.inferencer = inferencer;
    }

    @Override
    public AST ast() {
        return this.node().ast();
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public boolean is(Outline another) {
        if(another instanceof Lazy) {
            return this.node() == another.node();
        }else{
            return Projectable.super.is(another);
        }
    }

    @Override
    public Outline eventual() {
        this.node.ast().symbolEnv().enter(this.scope);
        Outline eventual = this.node.acceptInfer(inferencer);
        if (eventual instanceof ReferAble && !this.referencesProjections.isEmpty()) {
            eventual = ((ReferAble) eventual).project(this.referencesProjections);
        }
        if(!genericProjections.isEmpty() && eventual instanceof Projectable){
            AtomicReference<Outline> outline = new AtomicReference<>(eventual);
            genericProjections.forEach((s,p)->{
                p.projections().forEach((k,v)->{
                    outline.set(((Projectable) outline.get()).project(k,v,s));

                });
            });
            eventual = outline.get();
        }
        if(eventual instanceof ProductADT) {
            eventual.updateThis((ProductADT) eventual);
        }
//        if(this.me!=null){
//            eventual.updateThis(this.me);
//        }
        this.node.ast().symbolEnv().exit();
        return eventual;
    }

    @Override
    public boolean containsLazyAble() {
        return true;
    }

    @Override
    public String toString() {
        String ref = this.referencesProjections.stream().map(OutlineWrapper::toString).collect(Collectors.joining(","));
        if (!ref.trim().isEmpty()) {
            ref = "<" + ref + ">";
        }
        return "Lazy{" + this.node.getClass().getSimpleName() + ref + ")";
    }

    @Override
    public List<Reference> references() {
        return List.of();
    }

    @Override
    public Outline project(List<OutlineWrapper> projections) {
        this.referencesProjections = projections;
        return this;
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        List<OutlineWrapper> refs = this.referencesProjections;
        this.referencesProjections = new ArrayList<>();
        for (OutlineWrapper ref : refs) {
            if(ref.outline().id()==reference.id()) {
                this.referencesProjections.add(projection);
            }else{
                this.referencesProjections.add(ref);
            }
        }
        return this;
    }

    @Override
    public Outline copy() {
        Lazy lazy = new Lazy(this.node,this.inferencer);
        lazy.referencesProjections.addAll(this.referencesProjections);
        return lazy;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        GenericProjection p = this.genericProjections.get(session);
        if(p==null) {
            this.genericProjections.put(session, new GenericProjection(session));
            p = this.genericProjections.get(session);
        }
        p.add(projected,projection);
        return this;
    }

//    @Override
//    public void updateThis(ProductADT me) {
//        this.me = me;
//    }

    @Override
    public Outline guess() {
        return this;
    }

    @Override
    public boolean emptyConstraint() {
        return true;
    }

    @Override
    public boolean containsGeneric() {
        return false;
    }

}

class GenericProjection {
    private final ProjectSession session;
    private Map<Projectable,Outline> projections = new HashMap<>();

    public GenericProjection(ProjectSession session) {
        this.session = session;
    }

    public ProjectSession session(){
        return this.session;
    }

    public void add(Projectable projected, Outline projection) {
        this.projections.put(projected,projection);
    }

    public Map<Projectable,Outline> projections(){
        return this.projections;
    }
}
