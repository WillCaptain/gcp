package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.typeable.FunctionTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

/**
 * 函数参数在函数体内以函数方式调用产生的函数定义
 * x->x(10), 此时x.defined_to_be = higher order function
 */
public class HigherOrderFunction extends Function<Node, Outline> {
    public static HigherOrderFunction from(FunctionTypeNode node, Returnable returns, Outline... args) {
        if (args.length > 1) {
            Outline arg = args[0];
            Outline[] rests = new Outline[args.length - 1];
            for (int i = 0; i < rests.length; i++) {
                rests[i] = args[i + 1];
            }
            Returnable r = Return.from(from(null, returns, rests));
//            r.addNothing();
            return new HigherOrderFunction(null, arg, r);
        } else {
//            Return r = Return.from(returns);
//            r.addNothing();
            return new HigherOrderFunction(node, args[0], returns);
        }
    }

    public HigherOrderFunction(Node node, Outline argument, Returnable returns) {
        super(node, argument, returns);
//        this.argument = Nothing;
        returns.addReturn(Nothing);//.setInferred();
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline argument = this.argument instanceof Genericable<?,?> ?
                ((Genericable<?,?>) this.argument).project(projected, projection, session) : this.argument;
        Returnable returns = this.returns();
        if (this.returns().definedToBe() instanceof HigherOrderFunction) {
            returns = Return.from(returns.node(), returns.declaredToBe());
//            returns.addDefinedToBe(((HigherOrderFunction) this.returns().definedToBe()).doProject(projected, projection, session));
            returns.addDefinedToBe(((HigherOrderFunction) this.returns().definedToBe()).project(projected, projection, session));
        }
        return new HigherOrderFunction(this.node, argument, returns);
    }

    @Override
    public HigherOrderFunction copy() {
        return new HigherOrderFunction(this.node, this.argument, this.returns);
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (!(another instanceof Function<?, ?>)) return false;
        Function<?, ?> you = cast(another);
        return this.argument.is(you.argument) && (this.returns.toString().equals("`null`") || you.returns.is(this.returns));
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        Outline arg = this.argument();
        Returnable ret = this.returns();
        arg = cast(arg.project(reference, projection));
        ret = Return.from(ret.project(reference, projection));
        return new HigherOrderFunction(this.node, arg, ret);
    }
}
