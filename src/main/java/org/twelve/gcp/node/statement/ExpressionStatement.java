package org.twelve.gcp.node.statement;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * wrap an expression to make it a return ignore statement
 */
public class ExpressionStatement extends Statement{
    private final List<Expression> expressions = new ArrayList<>();

    public ExpressionStatement(Expression... expressions) {
        super(expressions[0].ast());
        for (Expression expr : expressions) {
            this.expressions.add(expr);
            this.addNode(expr);
        }
    }

    public List<Expression> expressions() {
        return this.expressions;
    }

    @Override
    public Outline accept(Inferences inferences) {
        this.expressions.forEach(e->e.infer(inferences));
        return this.ast().Ignore;
    }

    @Override
    public String lexeme() {
        String lexeme = this.expressions.stream().map(e -> e.lexeme()).collect(Collectors.joining(", "));
        return lexeme+";";
    }
}
