package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.decorators.Lazy;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;

import static org.twelve.gcp.common.Tool.*;

public class ReferenceCallInference implements Inference<ReferenceCallNode>{
    @Override
    public Outline infer(ReferenceCallNode node, Inferencer inferencer) {
        if(inferencer.isLazy() && isInFunction(node) && isInMember(node)) {
            return new Lazy(node,node.ast().inferences());
        }

        Outline hostOutline = cast(node.host().infer(inferencer));
        if(hostOutline instanceof ReferAble){
            ReferAble referAble = cast(((ReferAble)hostOutline).copy());
            return referAble.project(node.types().stream().map(t->new OutlineWrapper(node,t.infer(inferencer))).toList());
        }

        // External-builder pattern: __name__<Type> constructors (e.g. __ontology_repo__<Countries>,
        // __ontology_memo__<Employee>, __external_builder__<T>).
        // The host identifier is unknown at type-inference time (it is registered only at
        // interpreter runtime via registerConstructor), but the SEMANTIC contract is clear:
        // the constructor returns an instance of the type argument.
        // Returning the type-argument outline directly lets the type inferencer reason about
        // the full navigation chain (filter / edge traversal / ~this propagation) without
        // requiring a runtime environment during static analysis.
        if (hostOutline instanceof UNKNOWN && isExternalBuilder(node) && !node.types().isEmpty()) {
            return node.types().getFirst().infer(inferencer);
        }

        GCPErrorReporter.report(node, GCPErrCode.NOT_REFER_ABLE);
        return hostOutline;
    }

    /** Returns true when the host identifier follows the {@code __name__} external-builder convention. */
    private static boolean isExternalBuilder(ReferenceCallNode node) {
        String lexeme = node.host().lexeme();
        return lexeme != null && lexeme.startsWith("__") && lexeme.endsWith("__") && lexeme.length() > 4;
    }
}
