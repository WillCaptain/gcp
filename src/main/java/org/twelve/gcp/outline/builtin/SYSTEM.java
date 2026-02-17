package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.List;

public class SYSTEM implements Outline, ReferAble {

    private final long id;
    private final Identifier node;
    private List<Reference> references;

    public SYSTEM(Identifier identifier){
       this.id = identifier.ast().Counter.getAndIncrement();
       this.node = identifier;
   }
    @Override
    public AST ast() {
        return node.ast();
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public List<Reference> references() {
        return this.references;
    }

    @Override
    public Outline project(List<OutlineWrapper> types) {
        AST ast = this.ast();
        this.references = types.stream().map(t->Reference.from(ast,t.outline())).toList();

        return this.references.getFirst().declaredToBe();
    }

    @Override
    public Outline copy() {
        return this;
    }
}
