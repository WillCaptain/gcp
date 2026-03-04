package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.Selections;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.ERROR;
import org.twelve.gcp.outline.builtin.UNIT;

import java.util.ArrayList;
import java.util.List;

public class SelectionsInference implements Inference<Selections<?>>{
    @Override
    public Outline infer(Selections<?> node, Inferencer inferencer) {
        List<Outline> inferred = new ArrayList<>();
        for (Arm arm : node.arms()) {
            inferred.add(arm.infer(inferencer));
        }
        if(!node.containsElse() && !inferred.contains(node.ast().Ignore)){
            inferred.add(node.ast().Ignore);
        }
        //calculate return outline
        if(inferred.removeIf(o->o instanceof UNIT)){
            GCPErrorReporter.report(node, GCPErrCode.AMBIGUOUS_RETURN);
        }
        // Error from an arm is already reported at the arm level; strip it from the union
        // so it does not pollute Reference/Generic constraints in subsequent inference passes.
        if (!node.ast().asf().isLastInfer()) {
            inferred.removeIf(o -> o instanceof ERROR);
        }
        return Option.from(node,inferred.toArray(new Outline[]{}));
    }
}
