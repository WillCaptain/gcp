package org.twelve.gcp.node.namespace;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.builtin.Module;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.List;

public class ModuleNode extends Node {
    private final Identifier name;
    private NamespaceNode namespace = null;

    public ModuleNode(List<Identifier> source) {
        super(source.getFirst().ast(),null, new Module(source.getFirst().ast()));
        Identifier module = source.removeLast();
        if(!source.isEmpty()) {
            this.namespace = this.addNode(new NamespaceNode(module.ast(), source));
        }
        this.name = this.addNode(module);
    }

    @Override
    public String lexeme() {
        if(this.namespace==null) {
            return this.name.lexeme();
        }else {
            return this.namespace.lexeme()+"."+this.name.lexeme();
        }
    }

    public Identifier name(){
        return this.name;
    }

    public  NamespaceNode namespace(){
        return this.namespace;
    }

    @Override
    public boolean inferred() {
        return !(this.outline() instanceof UNKNOWN);
    }
}
