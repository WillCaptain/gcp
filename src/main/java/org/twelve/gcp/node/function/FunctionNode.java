package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.referable.ReferAbleNode;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

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
        return from(funcBody, refs, arguments, null);
    }

    /**
     * Build a (possibly curried) FunctionNode; attach {@code declaredReturn} to
     * the innermost function (the one wrapping the user's body) so return-type
     * checking applies to the actual return expression, not an intermediate
     * curry wrapper.
     */
    public static FunctionNode from(FunctionBody funcBody, List<ReferenceNode> refs,
                                    List<Argument> arguments, TypeNode declaredReturn) {
        AST ast = funcBody.ast();
        List<Argument> args = new ArrayList<>();
        for (Argument argument : arguments) {
//            args.add(argument.setIndex(args.size()));
            args.add(argument);
        }

        if (args.isEmpty()) {
            args.add(Argument.unit(ast));
        }
        Argument arg;//args.removeLast();
        FunctionNode function = null;// new FunctionNode(arg, funcBody);
        FunctionBody body = null;
        boolean firstIter = true;
        while (true) {
            arg = args.removeLast();
            if (body == null) {
                body = funcBody;
            } else {
                body = new FunctionBody(ast);
                body.addStatement(new ReturnStatement(function));
            }
            FunctionNode created;
            if (args.isEmpty()) {
                created = new FunctionNode(arg, body, refs.toArray(ReferenceNode[]::new));
            } else {
                created = new FunctionNode(arg, body);
            }
            if (firstIter) {
                created.withDeclaredReturn(declaredReturn);
                firstIter = false;
            }
            function = created;
            if (args.isEmpty()) break;
        }
        return function;
    }

    private final Argument argument;
    private final FunctionBody body;
    /**
     * Optional, user-written return type annotation from lambda syntax
     * {@code (x:T) : R -> body}. Stored on the outermost FunctionNode only
     * (the one whose body is the user's body); for multi-arg lambdas that
     * are desugared into nested FunctionNodes by {@link #from}, inner nodes
     * have a null declaredReturn and only the outermost carries R.
     */
    private TypeNode declaredReturn;

    public FunctionNode(Argument argument, FunctionBody body, ReferenceNode... refs) {
        super(body.ast(),null);
        this.argument = this.addNode(argument);
        this.body = this.addNode(body);
        for (ReferenceNode ref : refs) {
            this.refs.add(this.addNode(ref));
        }

    }

    /** Attach user-written {@code :R} annotation. Called once, post-construction. */
    public FunctionNode withDeclaredReturn(TypeNode typeNode) {
        if (typeNode != null) {
            this.declaredReturn = this.addNode(typeNode);
        }
        return this;
    }

    public TypeNode declaredReturn() {
        return this.declaredReturn;
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
            return argument().lexeme() + body().lexeme();
        } else {
            return new StringBuilder().append("fn<")
                    .append(
                    this.refs.stream().map(Identifier::lexeme).collect(Collectors.joining(",")))
                    .append(">")
                    .append((argument().lexeme().startsWith("("))? argument.lexeme():("("+argument().lexeme()+")"))
//                    .append("->")
                    .append(body.lexeme())
                    .toString();
        }
    }

    @Override
    public Long scope() {
        return this.body().scope();
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public List<ReferenceNode> refs() {
        return this.refs;
    }

//    @Override
//    public boolean inferred() {
//        return this.outline.inferred();
//    }

}
