package org.twelve.gcp.outline.projectable;

import lombok.Getter;
import lombok.Setter;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;


/**
 * 正常定义的function
 */
public class FirstOrderFunction extends Function<FunctionNode, Generic> {

    @Setter
    @Getter
    private ProjectSession session;

    private FirstOrderFunction(FunctionNode node, Generic argument, Return returns) {
        super(node, argument, returns);
    }

    public static FirstOrderFunction from(FunctionNode node, Generic argument, Return returns) {
        return new FirstOrderFunction(node, argument, returns);
    }

    public static FirstOrderFunction from(Outline returns, Outline... args) {
        if (args.length > 1) {
            Outline arg = args[0];
            Outline[] rests = new Outline[args.length - 1];
            for (int i = 0; i < rests.length; i++) {
                rests[i] = args[i + 1];
            }
            Return r = Return.from(from(returns, rests));
            return new FirstOrderFunction(null, Generic.from(arg), r);
        } else {
            return new FirstOrderFunction(null, Generic.from(args[0]), Return.from(returns));
        }
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline argProjection = this.argument.project(projected, projection, session);
        Generic argument = argProjection instanceof Generic ? cast(argProjection) : Generic.from(argProjection);
        Outline r = this.returns.project(projected, projection, session);
        Return returns = (r instanceof Return) ? cast(r) : Return.from(this.node, this.returns.declaredToBe());
        return new FirstOrderFunction(this.node, argument, returns);
    }

    @Override
    public FirstOrderFunction copy() {
        return new FirstOrderFunction(this.node,this.argument,this.returns);
    }
}
