package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class FunctionNode extends Expression {
    public static FunctionNode from(FunctionBody funcBody, Argument... arguments) {
        AST ast = funcBody.ast();
        List<Argument> args = new ArrayList<>();
        for (Argument argument : arguments) {
            args.add(argument.setIndex(args.size()));
        }

        if (args.isEmpty()) {
            args.add(Argument.unit(ast));
        }
        Argument arg = args.removeLast();
        FunctionNode function = new FunctionNode(arg, funcBody);
        while (!args.isEmpty()) {
            arg = args.removeLast();
            FunctionBody body = new FunctionBody(ast);
            body.addStatement(new ReturnStatement(function));
            function = new FunctionNode(arg, body);
        }
        return function;
    }

    private final Argument argument;
    private final FunctionBody body;

    public FunctionNode(Argument argument, FunctionBody body) {
        super(body.ast(), null);
        this.argument = this.addNode(argument);
        this.body = this.addNode(body);
    }


    public Argument argument() {
        return argument;
    }

    public FunctionBody body() {
        return body;
    }

    @Override
    public String lexeme() {
        return argument().lexeme() + "->" + body().lexeme();
    }

    @Override
    public Long scope() {
        return this.body().scope();
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

//    @Override
//    public boolean inferred() {
//        return this.outline.inferred();
//    }

}
