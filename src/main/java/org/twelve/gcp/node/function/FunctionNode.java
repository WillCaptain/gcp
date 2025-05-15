package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionNode extends Expression {
    public static FunctionNode from(FunctionBody funcBody, Argument... arguments) {
        return from(funcBody, new ArrayList<>(), new ArrayList<>(Arrays.asList(arguments)));
//        AST ast = funcBody.ast();
//        List<Argument> args = new ArrayList<>();
//        for (Argument argument : arguments) {
//            args.add(argument.setIndex(args.size()));
//        }
//
//        if (args.isEmpty()) {
//            args.add(Argument.unit(ast));
//        }
//        Argument arg = args.removeLast();
//        FunctionNode function = new FunctionNode(arg, funcBody);
//        while (!args.isEmpty()) {
//            arg = args.removeLast();
//            FunctionBody body = new FunctionBody(ast);
//            body.addStatement(new ReturnStatement(function));
//            function = new FunctionNode(arg, body);
//        }
//        return function;
    }

    public static FunctionNode from(FunctionBody funcBody, List<Identifier> refs, List<Argument> arguments) {
        AST ast = funcBody.ast();
        List<Argument> args = new ArrayList<>();
        for (Argument argument : arguments) {
            args.add(argument.setIndex(args.size()));
        }

        if (args.isEmpty()) {
            args.add(Argument.unit(ast));
        }
        Argument arg;//args.removeLast();
        FunctionNode function = null;// new FunctionNode(arg, funcBody);
        FunctionBody body = null;
        while (true) {
            arg = args.removeLast();
            if (body == null) {
                body = funcBody;
            } else {
                body = new FunctionBody(ast);
                body.addStatement(new ReturnStatement(function));
            }
            if (args.isEmpty()) {
                function = new FunctionNode(arg, body, refs.toArray(Identifier[]::new));
                break;
            } else {
                function = new FunctionNode(arg, body);
            }
        }
        return function;
    }

    private final Argument argument;
    private final FunctionBody body;
    private final List<Identifier> refs = new ArrayList<>();

    public FunctionNode(Argument argument, FunctionBody body, Identifier... refs) {
        super(body.ast(), null);
        this.argument = this.addNode(argument);
        this.body = this.addNode(body);
        for (Identifier ref : refs) {
            this.refs.add(this.addNode(ref));
        }
    }


    public Argument argument() {
        return argument;
    }

    public FunctionBody body() {
        return body;
    }

    public List<Identifier> refs() {
        return this.refs;
    }

    @Override
    public String lexeme() {
        if (this.refs.isEmpty()) {
            return argument().lexeme() + "->" + body().lexeme();
        } else {
            return new StringBuilder().append("func<")
                    .append(
                    this.refs.stream().map(Identifier::name).collect(Collectors.joining(",")))
                    .append(">(")
                    .append(argument().lexeme())
                    .append(")->")
                    .append(body.lexeme())
                    .toString();
        }
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
