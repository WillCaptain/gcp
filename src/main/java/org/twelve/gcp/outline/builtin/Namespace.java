package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.Primitive;

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
        this.namespace = namespace.token();
        this.node = namespace;
        this.id = Counter.getAndIncrement();
    }

    @Override
    public ONode node() {
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
    public boolean reAssignable() {
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

    public Namespace parentNamespace() {
        return this.parentNamespace;
    }
}
