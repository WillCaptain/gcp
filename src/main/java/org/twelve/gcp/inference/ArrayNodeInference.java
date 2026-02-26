package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.ArrayNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.projectable.Function;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.ProjectSession;

import static org.twelve.gcp.common.Tool.cast;

public class ArrayNodeInference implements Inference<ArrayNode> {
    @Override
    public Outline infer(ArrayNode node, Inferencer inferencer) {
        if(node.isEmpty()){
            return Array.from(node,node.ast().Nothing);
        }
        if (node.values() == null) {
            return inferRange(node, inferencer);
        } else {
            return inferValues(node, inferencer);
        }
    }

    private Outline inferRange(ArrayNode node, Inferencer inferencer) {
        Outline outline = node.ast().Integer;
        //validate begin
        if (node.begin() != null) {
            Outline begin = node.begin().infer(inferencer);
            if (!begin.is(node.ast().Integer)) {
                GCPErrorReporter.report(node.begin(), GCPErrCode.NOT_INTEGER);
            }
        }
        //validate end
        Outline end = node.end().infer(inferencer);
        if (!end.is(node.ast().Integer)) {
            GCPErrorReporter.report(node.end(), GCPErrCode.NOT_INTEGER);
        }
        //validate step
        if (node.step() != null) {
            Outline step = node.step().infer(inferencer);
            if (!step.is(node.ast().Integer)) {
                GCPErrorReporter.report(node.step(), GCPErrCode.NOT_INTEGER);
            }
        }
        //infer processor
        if(node.processor()!=null){
            Outline processor = node.processor().infer(inferencer);
            if (processor instanceof Function<?, ?>) {
                Function<?, Genericable<?,?>> f = cast(processor);
                outline = f.returns().project(f.argument(), node.ast().Integer, new ProjectSession());
            } else {
                GCPErrorReporter.report(node.condition(), GCPErrCode.NOT_A_FUNCTION);
            }
        }
        //validate condition
        if (node.condition() != null) {
            Outline condition = node.condition().infer(inferencer);
            if (condition instanceof Function<?, ?>) {
                Function<?, Genericable<?,?>> f = cast(condition);
                if (!f.returns().supposedToBe().is(node.ast().Boolean)) {
                    GCPErrorReporter.report(node.condition(), GCPErrCode.CONDITION_IS_NOT_BOOL);
                }
            } else {
                GCPErrorReporter.report(node.condition(), GCPErrCode.NOT_A_FUNCTION);
            }
        }
        return Array.from(node, outline);
    }

    private Outline inferValues(ArrayNode node, Inferencer inferencer) {
        Expression[] values = node.values();
        Outline outline = node.ast().Any;
        for (Expression value : values) {
            Outline v = value.infer(inferencer);
//            if (outline == Outline.Nothing) {
//                outline = v;
//            } else {

                if (outline.is(v)) continue;
                if (v.is(outline)) {
                    outline = v;
                    continue;
                }
                GCPErrorReporter.report(value, GCPErrCode.OUTLINE_MISMATCH, v + " doesn't match " + outline);
//            }
        }
        return Array.from(node, outline);
    }
}
