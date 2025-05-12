package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.projectable.Return;

import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionBodyInference extends BodyInference<FunctionBody> {
    @Override
    public Outline infer(FunctionBody node, Inferences inferences) {
        Outline inferred = super.infer(node, inferences);
        if(inferred instanceof IGNORE) return Outline.Unit;
        if(inferred instanceof Option){
            if(((Option) inferred).options().removeIf(o->o instanceof IGNORE)){
                ((Option) inferred).options().add(Outline.Unit);
            }
        }
        Return returns = Return.from(node.parent());
        returns.addReturn(inferred);
//        returns.replaceIgnores();
        return returns;
    }
}
