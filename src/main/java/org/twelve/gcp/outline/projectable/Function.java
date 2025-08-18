package org.twelve.gcp.outline.projectable;

import lombok.NonNull;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;


public abstract class Function<T extends Node, A extends Outline> implements Projectable  {
    protected long id;
    protected final T node;

    protected A argument;
    protected Returnable returns;
    private AST ast;

    public Function(@NonNull T node, A argument, Returnable returns) {
        this(node,node.ast(),argument,returns);
    }
    public Function(AST ast, A argument, Returnable returns) {
        this(null,ast,argument,returns);
    }
    protected Function(T node, AST ast, A argument, Returnable returns) {
        this.node = node;
        this.ast = ast;
        this.id = ast.Counter.getAndIncrement();

        this.argument = argument;
        this.returns = returns;
        this.returns.setArgument(this.argument.id());
    }

    @Override
    public AST ast() {
        return this.ast;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (!(another instanceof Function)) return false;
        Function you = cast(another);
        //参数逆变，返回值协变
        return you.argument.is(this.argument) && this.returns.is(you.returns);
    }

    /**
     * funtion的equals不遵循参数逆变，返回值协变规则
     * 符合参数和返回值皆equals
     * @param another 目标outline
     * @return
     */
    @Override
    public boolean equals(Outline another) {
        if(!(another instanceof Function)) return false;
        Function you = cast(another);
        return this.argument.equals(you.argument) && this.returns.equals(you.returns);
    }

    public Returnable returns() {
        return this.returns;
    }

    public A argument() {
        return argument;
    }


    @Override
    public boolean inferred() {
        return this.argument.inferred() && this.returns.inferred();
    }

    @Override
    public String toString() {
//        return argument.toString()+"->"+returns.toString();
        return this.guess().toString();
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public Outline guess() {
        return new FixFunction(this.node(),this.ast(),
                this.argument instanceof Projectable?((Projectable) this.argument).guess():this.argument,
                this.returns.guess());
    }

    @Override
    public boolean containsUnknown() {
        return this.argument.containsUnknown()||this.returns.containsUnknown();
    }

    @Override
    public boolean emptyConstraint() {
        return (this.argument() instanceof Projectable && ((Projectable) this.argument()).emptyConstraint()) || this.returns.emptyConstraint();
    }

    @Override
    public boolean containsGeneric() {
        return true;
    }
}
