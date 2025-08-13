package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.projectable.Return;
import org.twelve.gcp.outline.projectable.Returnable;

public class FunctionBodyInference extends BodyInference<FunctionBody> {
    @Override
    public Outline infer(FunctionBody node, Inferences inferences) {
        Outline inferred = super.infer(node, inferences);
        if(inferred instanceof IGNORE) return node.ast().Unit;
        if(inferred instanceof Option){
            if(((Option) inferred).options().removeIf(o->o instanceof IGNORE)){
                ((Option) inferred).options().add(node.ast().Unit);
            }
        }
        Returnable returns = Return.from(node.parent());
        returns.addReturn(inferred);
//        returns.replaceIgnores();
        return returns;
    }
}
