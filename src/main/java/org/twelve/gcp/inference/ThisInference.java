package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.ThisNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.decorators.This;
import org.twelve.gcp.outlineenv.AstScope;

import static org.twelve.gcp.common.Tool.cast;

public class ThisInference implements Inference<ThisNode> {
    @Override
    public Outline infer(ThisNode node, Inferencer inferencer) {
        return findEntity(node);
    }

    public static Outline findEntity(AbstractNode node) {
        AstScope scope = node.ast().symbolEnv().current();
        while(scope.scopeType()!= SCOPE_TYPE.IN_PRODUCT_ADT){
            scope = scope.parent();
            if(scope==null){
                GCPErrorReporter.report(node, GCPErrCode.UNAVAILABLE_THIS);
                return node.ast().Error;
            }
        }
        if(scope.outline() instanceof Entity) {
            return new This(cast(scope.outline()));
        }else{
            return scope.outline();
        }
    }
}
