package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.Selections;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.UNIT;

import java.util.ArrayList;
import java.util.List;

public class SelectionsInference implements Inference<Selections>{
    @Override
    public Outline infer(Selections node, Inferences inferences) {
        List<Outline> inferred = new ArrayList<>();
        for (Arm arm : node.arms()) {
            inferred.add(arm.infer(inferences));
        }
        if(!node.containsElse() && !inferred.contains(Outline.Ignore)){
            inferred.add(Outline.Ignore);
        }
        //calculate return outline
//        if(inferred.stream().allMatch(o->o instanceof UNIT)) return Outline.Ignore;//can't be assigned and will not be return statement
        if(inferred.removeIf(o->o instanceof UNIT)){

            ErrorReporter.report(node, GCPErrCode.AMBIGUOUS_RETURN);
        }
        return Option.from(node,inferred.toArray(new Outline[]{}));
    }
}
