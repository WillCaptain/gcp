package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionCallNode extends Expression {
    private Expression function;
    private List<Expression> arguments;

    public FunctionCallNode(OAST ast, Token funcName, Expression... arguments) {
        this(ast, new Identifier(ast, funcName), arguments);
    }

    public FunctionCallNode(OAST ast, Expression function, Expression... arguments) {
        super(ast, null);
        this.function = function;
        this.addNode(this.function);
        this.arguments = Arrays.asList(arguments);
        for (Expression argument : this.arguments) {
            this.addNode(argument);
        }
    }

    public Expression function() {
        return this.function;
    }

    public List<Expression> arguments() {
        return this.arguments;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public boolean inferred() {
        return this.outline.inferred();
    }

    @Override
    public String lexeme() {
        String args = this.arguments.stream().map(a -> a.lexeme()).collect(Collectors.joining(","));
        return function.lexeme() + "(" + args + ")" + (this.outline == Outline.Unknown ? "" : (" : " + this.outline.toString()));
    }
}
