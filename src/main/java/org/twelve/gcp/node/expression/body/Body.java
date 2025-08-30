package org.twelve.gcp.node.expression.body;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.outline.Outline;

public abstract class Body extends Expression {
    private final Long scope;

    public Body(AST ast) {
        super(ast, null);
        this.scope = ast.scopeIndexer().incrementAndGet();
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public Long scope() {
        return this.scope;
    }

    public <T extends Statement> T addStatement(T statement) {
        return this.addNode(statement);
    }

    @Override
    public String lexeme() {
        String[] lines = super.lexeme().split("\n");
        StringBuilder sb = new StringBuilder("{\n");
        for (String line : lines) {
            sb.append("  " + line + "\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
