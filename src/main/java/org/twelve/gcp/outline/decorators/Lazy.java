package org.twelve.gcp.outline.decorators;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.AstScope;

public class Lazy implements Outline {
    private final long id;
    private final Node node;
    private final Inferences inferences;
    private final AstScope scope;

    public Lazy(Node node, Inferences inferences) {
        this.id = node.ast().Counter.getAndIncrement();
        this.node = node;
        this.scope = node.ast().symbolEnv().current();
        this.inferences = inferences;
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
        return this.node()==another.node();
    }

    @Override
    public Outline eventual() {
        this.node.ast().symbolEnv().enter(this.scope);
        Outline eventual = this.node.accept(inferences);
        this.node.ast().symbolEnv().exit();
        return eventual;
    }

    @Override
    public String toString() {
        return "Lazy{" +this.node.getClass().getSimpleName().split(":")[0] +")";
    }
}
