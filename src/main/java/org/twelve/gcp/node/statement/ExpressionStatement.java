package org.twelve.gcp.node.statement;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

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
    public Outline acceptInfer(Inferencer inferencer) {
        this.expressions.forEach(e->e.infer(inferencer));
        return this.ast().Ignore;
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    @Override
    public String lexeme() {
        String lexeme = this.expressions.stream().map(e -> e.lexeme()).collect(Collectors.joining(", "));
        return lexeme+";";
    }
}
