package org.twelve.gcp.outline.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.unpack.UnpackNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;

public class Unpack implements Outline {
    private final UnpackNode node;
    private final Outline base;

    public Unpack(UnpackNode node, Outline base) {
        this.node = node;
        this.base = base;
    }
    public Unpack(UnpackNode node) {
        this(node,node.ast().Nothing);
    }

    @Override
    public AST ast() {
        return this.node.ast();
    }

    @Override
    public UnpackNode node() {
        return this.node;
    }

    @Override
    public boolean is(Outline another) {
        if(another instanceof Entity) {
            return this.base.is(((Entity) another).base());
        }else{
            return false;
        }
    }

    @Override
    public long id() {
        return -1;
    }

    @Override
    public String toString() {
        return node.toString();
    }
}
