package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

public class FixFunction  implements Projectable {
    protected long id;
    protected final Node node;
    private final Outline argument;
    private final Outline returns;

    public FixFunction(Node node, Outline arg, Outline returns) {
        this.node = node;
        this.id = Counter.getAndIncrement();

        this.argument = arg;
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
    public Node node() {
        return this.node;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        return this.returns;
    }

    @Override
    public Outline guess() {
        Outline arg = argument instanceof Projectable?((Projectable) argument).guess():argument;
        Outline r = returns instanceof Projectable?((Projectable) returns).guess():returns;
        return new FixFunction(node,arg,r);
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
//        if(!(another instanceof Function)) return false;
//        Function you = cast(another);
        //参数逆变，返回值协变
//        return (you.argument instanceof ANY || you.argument.is(this.argument)) && this.returns.is(you.returns);
        return (you.argument.toString().equals(CONSTANTS.ANY_STR)||you.argument.canBe(this.argument)) && this.returns.is(you.returns);
    }

//    @Override
//    public FixFunction copy() {
//        return new FixFunction(node,argument,returns);
//    }
}
