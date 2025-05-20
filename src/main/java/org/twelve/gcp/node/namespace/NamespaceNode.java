package org.twelve.gcp.node.namespace;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.WrapperTypeNode;
import org.twelve.gcp.outline.builtin.Namespace;

import java.util.List;

public class NamespaceNode extends Node {
    public NamespaceNode(AST ast, List<Identifier> names) {
        super(ast, null);
        Namespace outline = null;
        for (Identifier name : names) {
            outline = new Namespace(name, outline);
            this.addNode(new Variable(name,false, new WrapperTypeNode(ast,outline,name.loc())));
        }
    }

    @Override
    public Location loc() {
        if (this.nodes().isEmpty()) {
            return new SimpleLocation(0, 0);
        } else {
            return new SimpleLocation(this.nodes().getFirst().loc().start(),
                    this.nodes().getLast().loc().end());
        }
    }

    @Override
    public String lexeme() {
        StringBuilder result = new StringBuilder();
        for (Node node : this.nodes()) {
            result.append(".").append(node.lexeme());
        }
        return result.isEmpty() ? "" : result.substring(1);
    }
}
