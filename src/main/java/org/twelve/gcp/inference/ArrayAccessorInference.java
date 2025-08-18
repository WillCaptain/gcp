package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.accessor.ArrayAccessor;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.adt.Dict;
import org.twelve.gcp.outline.adt.DictOrArray;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;

import static org.twelve.gcp.common.Tool.cast;

public class ArrayAccessorInference implements Inference<ArrayAccessor> {
    @Override
    public Outline infer(ArrayAccessor node, Inferences inferences) {
        Outline array = node.array().infer(inferences);
        Outline index = node.index().infer(inferences);
        Outline inferred = this.inferArray(node, array, index);
        if (inferred == null) {
            inferred = this.inferDict(node, array, index);
        }
        if (inferred == null) {
            inferred = this.inferArrayOrDict(node, array, index);
        }
        return inferred;
        /*if (array instanceof Array) {
            if (index instanceof Genericable<?, ?>) {
                ((Genericable<?, ?>) index).addDefinedToBe(Outline.Long);
            }
            if (!(index.is(Outline.Long))) {
                ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, index + " doesn't match integer or long");
            }
            return ((Array) array).itemOutline();
        }
        if (array instanceof Dict) {
            Dict dict = cast(array);
            if (index instanceof Genericable<?, ?>) {
                ((Genericable<?, ?>) index).addDefinedToBe(dict.key());
            }
            if (!(index.is(dict.key()))) {
                ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, index + " doesn't match map key");
            }
        }

        if (array instanceof Genericable) {
            Genericable gen = cast(array);
            if (gen.max() instanceof Array || gen.min() instanceof Array) {
                if (index instanceof Genericable<?, ?>) {
                    ((Genericable<?, ?>) index).addDefinedToBe(Outline.Long);
                }
            }
            Genericable<?, ?> itemOutline = Generic.from(node, null);
            ((Genericable<?, ?>) array).addDefinedToBe(new Array(node, itemOutline));
            return itemOutline;
        }
        ErrorReporter.report(node, GCPErrCode.NOT_AN_ARRAY_OR_DICT);
        return Outline.Unknown;*/
    }

    private Outline inferArrayOrDict(ArrayAccessor node, Outline host, Outline index) {
        if (!(host instanceof Genericable<?, ?>)) {
            return node.ast().Error;
        }
        Genericable<?, ?> gen = cast(host);
        if (gen.emptyConstraint()) {
            Outline key = index;//Generic.from(node, null);
            Genericable<?, ?> value = Generic.from(node, null);
            gen.addDefinedToBe(new DictOrArray<>(node, node.ast(), node.ast().Nothing.buildIn(), key, value));
            return value;
        } else {
            GCPErrorReporter.report(node, GCPErrCode.NOT_AN_ARRAY_OR_DICT);
            return node.ast().Unknown;
        }
    }

    private Outline inferArray(ArrayAccessor node, Outline host, Outline index) {
        if (host instanceof Array) {
            if (index instanceof Genericable<?, ?>) {
                ((Genericable<?, ?>) index).addDefinedToBe(node.ast().Long);
            }
            if (!(index.is(node.ast().Long))) {
                GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, index + " doesn't match integer or long");
            }
            return ((Array) host).itemOutline();
        }
        if (host instanceof Genericable<?, ?>) {
            Genericable<?, ?> gen = cast(host);
            if (gen.max() instanceof Array || gen.min() instanceof Array) {
                if (index instanceof Genericable<?, ?>) {
                    ((Genericable<?, ?>) index).addDefinedToBe(node.ast().Long);
                }
                Genericable<?, ?> itemOutline = Generic.from(node, null);
                gen.addDefinedToBe(Array.from(node, itemOutline));
                return itemOutline;
            }
        }
        return null;
    }

    private Outline inferDict(ArrayAccessor node, Outline host, Outline index) {
        if (host instanceof Dict) {
            Dict dict = cast(host);
            if (index instanceof Genericable<?, ?>) {
                ((Genericable<?, ?>) index).addDefinedToBe(dict.key());
            }
            if (!(index.is(dict.key()))) {
                GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, index + " doesn't match map key");
            }
        }
        if (host instanceof Genericable<?, ?>) {
            Genericable<?, ?> gen = cast(host);
            if (gen.max() instanceof Dict || gen.min() instanceof Dict) {
                Outline key = (gen.max() instanceof Dict) ? ((Dict) gen.max()).key() : null;
                key = (gen.min() instanceof Dict) ? ((Dict) gen.min()).key() : key;
                if (index instanceof Genericable<?, ?>) {
                    ((Genericable<?, ?>) index).addDefinedToBe(key);
                }
                Genericable<?, ?> value = Generic.from(node, null);
                gen.addDefinedToBe(new Dict(node, key, value));
                return value;
            }
        }
        return null;
    }
}


