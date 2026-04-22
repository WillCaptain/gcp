package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.projectable.*;

import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionInference implements Inference<FunctionNode> {
    @Override
    public Outline infer(FunctionNode node, Inferencer inferencer) {
        List<Reference> refs = node.refs().stream().map(r->(Reference)r.infer(inferencer)).toList();
        Outline argOutline = node.argument().infer(inferencer);
        Genericable<?,?> argument = argOutline instanceof Genericable<?,?>
                ? cast(argOutline)
                : Generic.from(node.argument(),
                        argOutline instanceof Projectable
                                ? ((Projectable) argOutline).guess()
                                : argOutline);
        Returnable returns = cast(node.body().infer(inferencer));
        // Apply user-written `:R` annotation from lambda syntax
        // `(x:T):R -> body`. R lives on the innermost FunctionNode only;
        // see FunctionNode.from(..., declaredReturn) for the attachment point.
        //
        // We enforce R post-hoc, after body inference fully populates
        // `Return.supposed`: an early constraint via Return's constructor
        // rejects mid-inference intermediate types (e.g. the Addable/Multipliable
        // placeholder of `x*100`) before they narrow to Int, causing the let
        // binding to silently lose its value. Post-hoc avoids that window.
        if (node.declaredReturn() != null) {
            Outline declaredR = node.declaredReturn().infer(inferencer);
            if (!(declaredR instanceof ANY) && returns instanceof Return r) {
                Outline actual = r.supposedToBe();
                if (actual instanceof Projectable p) {
                    actual = p.guess();
                }
                // Skip transient unresolved intermediates (recursive self-refs
                // whose sibling branch has not yet narrowed, Pending, etc.);
                // the next inference round will re-enter here with concrete
                // operands. `containsUnknown` covers both UNKNOWN and Pending.
                boolean transient_ = actual == null
                        || actual.containsUnknown()
                        || actual instanceof Projectable;
                if (!transient_ && !actual.is(declaredR)) {
                    GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH,
                            actual + " mismatch with declared return " + declaredR);
                }
            }
        }
        return FirstOrderFunction.from(node,argument,returns,refs);
    }
}
