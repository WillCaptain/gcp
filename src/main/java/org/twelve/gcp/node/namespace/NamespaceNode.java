package org.twelve.gcp.node.namespace;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.builtin.Namespace;

import java.util.List;

public class NamespaceNode extends ONode {
    public NamespaceNode(OAST ast, List<Token> names) {
        super(ast, null);
        Namespace outline = null;
        for (Token name : names) {
            outline = new Namespace(new Identifier(ast,name), outline);
            this.addNode(new Identifier(ast, name, outline, false));
        }
    }

    @Override
    public Location loc() {
        if (this.nodes().size() == 0) {
            return new SimpleLocation(0, 0);
        } else {
            return new SimpleLocation(this.nodes().get(0).loc().start(),
                    this.nodes().get(this.nodes().size() - 1).loc().end());
        }
    }

    @Override
    public String lexeme() {
        StringBuilder result = new StringBuilder();
        for (Node node : this.nodes()) {
            result.append("." + node.lexeme());
        }
        return result.length() == 0 ? "" : result.substring(1);
    }
}
