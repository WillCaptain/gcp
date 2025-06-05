package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.referable.ReferAbleNode;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * function, map, array, entity
 * are able to have reference type sub structure
 */
public class FunctionNode extends Expression implements ReferAbleNode {
    private final List<ReferenceNode> refs = new ArrayList<>();

    public static FunctionNode from(FunctionBody funcBody, Argument... arguments) {
        return from(funcBody, new ArrayList<>(), new ArrayList<>(Arrays.asList(arguments)));
    }

    public static FunctionNode from(FunctionBody funcBody, List<ReferenceNode> refs, List<Argument> arguments) {
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
                function = new FunctionNode(arg, body, refs.toArray(ReferenceNode[]::new));
                break;
            } else {
                function = new FunctionNode(arg, body);
            }
        }
        return function;
    }

    private final Argument argument;
    private final FunctionBody body;

    public FunctionNode(Argument argument, FunctionBody body, ReferenceNode... refs) {
        super(body.ast(),null);
        this.argument = this.addNode(argument);
        this.body = this.addNode(body);
        for (ReferenceNode ref : refs) {
            this.refs.add(this.addNode(ref));
        }

    }


    public Argument argument() {
        return argument;
    }

    public FunctionBody body() {
        return body;
    }

    @Override
    public String lexeme() {
        if (this.refs.isEmpty()) {
            return argument().lexeme() + "->" + body().lexeme();
        } else {
            return new StringBuilder().append("fx<")
                    .append(
                    this.refs.stream().map(Identifier::lexeme).collect(Collectors.joining(",")))
                    .append(">")
                    .append((argument().lexeme().equals("()"))? argument.lexeme():("("+argument().lexeme()+")"))
                    .append("->")
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

    public List<ReferenceNode> refs() {
        return this.refs;
    }

//    @Override
//    public boolean inferred() {
//        return this.outline.inferred();
//    }

}
