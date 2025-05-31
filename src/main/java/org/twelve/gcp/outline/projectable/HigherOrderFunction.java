package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

/**
 * 函数参数在函数体内以函数方式调用产生的函数定义
 * x->x(10), 此时x.defined_to_be = higher order function
 */
public class HigherOrderFunction extends Function<Node, Outline> {
    public HigherOrderFunction(Node node, Outline argument, Return returns) {
        super(node, argument, returns);
        returns.addNothing();
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline argument = this.argument instanceof Generic ?
                ((Generic) this.argument).project(projected, projection, session) : this.argument;
        Return returns = this.returns();
        if (this.returns().definedToBe() instanceof HigherOrderFunction) {
            returns = Return.from(returns.node(), returns.declaredToBe());
//            returns.addDefinedToBe(((HigherOrderFunction) this.returns().definedToBe()).doProject(projected, projection, session));
            returns.addDefinedToBe(((HigherOrderFunction) this.returns().definedToBe()).project(projected, projection, session));
        }
        return new HigherOrderFunction(this.node, argument,returns);
    }

    @Override
    public HigherOrderFunction copy() {
        return new HigherOrderFunction(this.node,this.argument,this.returns);
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if(!(another instanceof Function<?,?>)) return false;
        Function<?,?> you = cast(another);
        return this.argument.is(you.argument) && (this.returns.toString().equals("`null`")||you.returns.is(this.returns));
    }
}
