package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.ArrayNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.builtin.Array_;
import org.twelve.gcp.outline.projectable.Function;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.ProjectSession;

import static org.twelve.gcp.common.Tool.cast;

public class ArrayNodeInference implements Inference<ArrayNode> {
    @Override
    public Outline infer(ArrayNode node, Inferences inferences) {
        if(node.isEmpty()){
            return new Array(node,Outline.Nothing);
        }
        if (node.values() == null) {
            return inferRange(node, inferences);
        } else {
            return inferValues(node, inferences);
        }
    }

    private Outline inferRange(ArrayNode node, Inferences inferences) {
        Outline outline = Outline.Integer;
        //validate begin
        if (node.begin() != null) {
            Outline begin = node.begin().infer(inferences);
            if (!begin.is(Outline.Integer)) {
                ErrorReporter.report(node.begin(), GCPErrCode.NOT_INTEGER);
            }
        }
        //validate end
        Outline end = node.end().infer(inferences);
        if (!end.is(Outline.Integer)) {
            ErrorReporter.report(node.end(), GCPErrCode.NOT_INTEGER);
        }
        //validate step
        if (node.step() != null) {
            Outline step = node.step().infer(inferences);
            if (!step.is(Outline.Integer)) {
                ErrorReporter.report(node.step(), GCPErrCode.NOT_INTEGER);
            }
        }
        //infer processor
        if(node.processor()!=null){
            Outline processor = node.processor().infer(inferences);
            if (processor instanceof Function<?, ?>) {
                Function<?, Genericable<?,?>> f = cast(processor);
                outline = f.returns().project(f.argument(), Outline.Integer, new ProjectSession());
            } else {
                ErrorReporter.report(node.condition(), GCPErrCode.NOT_A_FUNCTION);
            }
        }
        //validate condition
        if (node.condition() != null) {
            Outline condition = node.condition().infer(inferences);
            if (condition instanceof Function<?, ?>) {
                Function<?, Genericable<?,?>> f = cast(condition);
                if (!f.returns().supposedToBe().is(Outline.Boolean)) {
                    ErrorReporter.report(node.condition(), GCPErrCode.CONDITION_IS_NOT_BOOL);
                }
            } else {
                ErrorReporter.report(node.condition(), GCPErrCode.NOT_A_FUNCTION);
            }
        }
        return new Array(node, outline);
    }

    private Outline inferValues(ArrayNode node, Inferences inferences) {
        Expression[] values = node.values();
        Outline outline = Outline.Any;
        for (Expression value : values) {
            Outline v = value.infer(inferences);
//            if (outline == Outline.Nothing) {
//                outline = v;
//            } else {

                if (outline.is(v)) continue;
                if (v.is(outline)) {
                    outline = v;
                    continue;
                }
                ErrorReporter.report(value, GCPErrCode.OUTLINE_MISMATCH, v + " doesn't match " + outline);
//            }
        }
        return new Array(node, outline);
    }
}
