package org.twelve.gcp.builder;

import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.statement.VariableDeclarator;

public class VariableDeclaratorBuilder {
    private final VariableDeclarator declarator;

    public VariableDeclaratorBuilder(VariableDeclarator declarator) {
        this.declarator = declarator;
    }

    public VariableDeclaratorBuilder declare(String id, Expression expression) {
        this.declarator.declare(new Identifier(this.declarator.ast(), new Token<>(id)), expression);
        return this;
    }

    public VariableDeclaratorBuilder declare(String id, TypeNode declared, Expression expression) {
        this.declarator.declare(new Identifier(this.declarator.ast(), new Token<>(id)), declared, expression);
        return this;
    }

    public VariableDeclarator get() {
        return declarator;
    }
}
