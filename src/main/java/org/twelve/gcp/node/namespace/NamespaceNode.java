package org.twelve.gcp.node.namespace;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.builtin.Namespace;

import java.util.List;

public class NamespaceNode extends Node {
    public NamespaceNode(AST ast, List<Token<String>> names) {
        super(ast, null);
        Namespace outline = null;
        for (Token<String> name : names) {
            outline = new Namespace(new Identifier(ast,name), outline);
            this.addNode(new Identifier(ast, name, outline));
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
