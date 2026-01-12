package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.body.WithExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;

import static org.twelve.gcp.common.Tool.cast;

public class WithExpressionInference implements Inference<WithExpression>{
    @Override
    public Outline infer(WithExpression node, Inferences inferences) {
        Outline resource = node.resource().infer(inferences);
        if(resource instanceof Entity){
            Entity entity = cast(resource);
            checkResourceMethod(node, entity, "open");
            checkResourceMethod(node, entity, "close");
        }else{
            GCPErrorReporter.report(node.as(), GCPErrCode.OUTLINE_MISMATCH,"with expression should be entity");
        }
        if(node.as()!=null){
            node.ast().symbolEnv().defineSymbol(node.as().name(), resource, false, node.as());
            node.as().infer(inferences);
        }
        return node.body().infer(inferences);
    }

    private static void checkResourceMethod(WithExpression node, Entity entity, String name) {
        if(entity.getMember(name).isPresent()){
            if(!(entity.getMember(name).get().outline().toString().equals("()->()"))){
                GCPErrorReporter.report(node.as(), GCPErrCode.OUTLINE_MISMATCH, name +" method should return unit");
            }
        }else {
            GCPErrorReporter.report(node.as(), GCPErrCode.OUTLINE_MISMATCH,"with resource should have "+ name +" method");

        }
    }
}
