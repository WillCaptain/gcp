package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.accessor.ArrayAccessor;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;

public class ArrayAccessorInference implements Inference<ArrayAccessor> {
    @Override
    public Outline infer(ArrayAccessor node, Inferences inferences) {
        Outline index = node.index().infer(inferences);
        if(index instanceof Genericable<?,?>){
            ((Genericable<?,?>)index).addDefinedToBe(Outline.Long);
        }
        if (!(index.is(Outline.Long))) {
            ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, index + " doesn't match integer or long");
        }
        Outline array = node.array().infer(inferences);

        if (array instanceof Array) {
            return ((Array) array).itemOutline();
        }
        if (array instanceof Genericable) {
            Genericable<?,?> itemOutline = Generic.from(node,null);
            ((Genericable<?,?>)array).addDefinedToBe(new Array(node,itemOutline));
            return itemOutline;
        }
        ErrorReporter.report(node, GCPErrCode.NOT_AN_ARRAY);
        return Outline.Unknown;
    }
}
