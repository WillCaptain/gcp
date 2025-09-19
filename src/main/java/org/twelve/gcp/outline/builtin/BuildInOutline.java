package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.outline.Outline;

public abstract class BuildInOutline implements Outline {
    private final AST ast;

    protected BuildInOutline(AST ast) {
        this.ast = ast;
    }

    public boolean tryIamYou(Outline another) {
        if (another instanceof UNKNOWN) return true;
        Class<?> yourClass = another.getClass();
        return yourClass.isInstance(this);
    }

    @Override
    public AbstractNode node(){
        return null;
    }

    @Override
    public AST ast() {
        return ast;
    }
}
