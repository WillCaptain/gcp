package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

public class FixFunction  extends Function<Node,Outline> {
    protected long id;
    private final Outline returns;

    public FixFunction(Node node, AST ast, Outline arg, Outline returns) {
        super(node,ast,arg,Return.from(ast,returns));
        this.returns = returns;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public String toString() {
        FixFunction guess = cast(this.guess());
        String arg = guess.argument.toString();
        if(guess.argument instanceof FixFunction){
            arg = "("+arg+")";
        }
        return arg + "->" + guess.returns.toString();
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        return this.returns;
    }

    @Override
    public Outline guess() {
        Outline arg = argument instanceof Projectable?((Projectable) argument).guess():argument;
        Outline r = returns instanceof Projectable?((Projectable) returns).guess():returns;
        return new FixFunction(node,ast(),arg,r);
    }

    @Override
    public boolean emptyConstraint() {
        return (this.argument instanceof Projectable && ((Projectable) this.argument).emptyConstraint()) ||
                ((this.returns instanceof Projectable && ((Projectable) this.returns).emptyConstraint()));
    }

    @Override
    public boolean containsGeneric() {
        return (this.argument instanceof Projectable && ((Projectable) this.argument).containsGeneric()) ||
                ((this.returns instanceof Projectable && ((Projectable) this.returns).containsGeneric()));
    }

    @Override
    public boolean tryIamYou(Outline another) {
        Function you = null;
        if(another instanceof Function){
            you = cast(another);
        }
        if(another instanceof Genericable<?,?>){
            you = cast(((Genericable<?,?>)another).min());
        }
        if(you==null) return false;
        //参数逆变，返回值协变
        return (you.argument.toString().equals(CONSTANTS.ANY_STR)||you.argument.canBe(this.argument)) && this.returns.is(you.returns);
    }
}
