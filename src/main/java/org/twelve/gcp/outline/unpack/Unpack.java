package org.twelve.gcp.outline.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.unpack.TupleUnpackNode;
import org.twelve.gcp.node.unpack.UnpackNode;
import org.twelve.gcp.outline.Outline;

public class Unpack implements Outline {
    private final UnpackNode node;

    public Unpack(UnpackNode node) {
        this.node = node;
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
    public long id() {
        return -1;
    }
}
