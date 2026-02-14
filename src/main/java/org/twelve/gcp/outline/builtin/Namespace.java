package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class Namespace implements Outline {
    private final String namespace;
    private final List<Namespace> subNamespaces = new ArrayList<>();
    private final Namespace parentNamespace ;
    private final long id;
    private final Identifier node;

    public Namespace(Identifier namespace, Namespace parent) {
        this.parentNamespace = parent;
        if(parent!=null) {
            parent.subNamespaces.add(this);
        }
        this.namespace = namespace.name();
        this.node = namespace;
        this.id = this.node.ast().Counter.getAndIncrement();
    }

    @Override
    public AST ast() {
        return this.node.ast();
    }

    @Override
    public AbstractNode node() {
        return this.node;
    }

    @Override
    public boolean is(Outline another) {
        return false;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public boolean beAssignedAble() {
        return false;
    }

    public boolean isTop(){
        return this.parentNamespace==null;
    }

    public String namespace() {
        return this.namespace;
    }

    public List<Namespace> subNamespaces() {
        List<Namespace> subs = new ArrayList<>();
        subs.addAll(this.subNamespaces);
        return subs;
    }

    @Override
    public String toString() {
        return "";
    }

    public Namespace parentNamespace() {
        return this.parentNamespace;
    }
}
