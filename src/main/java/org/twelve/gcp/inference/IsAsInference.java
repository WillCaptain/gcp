package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.IsAs;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.projectable.Genericable;

public class IsAsInference implements Inference<IsAs> {
    @Override
    public Outline infer(IsAs node, Inferencer inferencer) {
        Outline lhs = node.a().infer(inferencer);
        Outline b = node.b().infer(inferencer);
        boolean mayCast = b.canBe(lhs);
        // Nullable declaration compatibility:
        // For `x:T?`, declaredToBe is Option(T, Nothing). In this case `x is Nothing`
        // should remain a valid runtime check even when Genericable hasn't fully projected.
        if (!mayCast && lhs instanceof Genericable<?, ?> generic
                && "Nothing".equals(node.b().lexeme())
                && generic.declaredToBe() instanceof Option opt
                && opt.options().stream().anyMatch(o -> o instanceof NOTHING)) {
            mayCast = true;
        }
        if (!mayCast) {
            GCPErrorReporter.report(node, GCPErrCode.TYPE_CAST_NEVER_SUCCEED, node.a() + " will never be " + node.b());
        } else {
            if (lhs instanceof Genericable<?, ?>) {
//                ((Genericable<?,?>) lhs).addCouldBe(node.b());//its a hint to indicate it is possible to be
            }
        }
        //if(node.parent() instanceof Arm){
        //node.ast().symbolEnv().enter(((Arm) node.parent()).consequence());
        Identifier bind = node.c();
        if (bind != null) {
            node.ast().symbolEnv().defineSymbol(bind.name(), b, false, bind);
            bind.infer(inferencer);
        }
        //node.ast().symbolEnv().exit();;
        //}
        return node.ast().Boolean;
    }
}
