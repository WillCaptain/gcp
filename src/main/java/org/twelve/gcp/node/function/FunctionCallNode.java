package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionCallNode extends Expression {
    private Expression function;
    private List<Expression> arguments;

    public FunctionCallNode(Expression function, Expression... arguments) {
        super(function.ast(), null);
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
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public boolean inferred() {
        boolean result =  this.outline.inferred();
        if(!result) this.ast().missInferred().add(this);
        return result;
    }

    @Override
    public String lexeme() {
        String args = this.arguments.stream().map(AbstractNode::lexeme)
                .collect(Collectors.joining(","));
        return function.lexeme() + "(" + args + ")";// + ((this.outline  instanceof UNKNOWN) ? "" : (" : " + this.outline.toString()));
    }
    @Override
    public void clearError() {
        super.clearError();
        for (Node node : this.nodes()) {
            node.clearError();
        }
    }
}
