package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.ThisNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.AstScope;

public class ThisInference implements Inference<ThisNode> {
    @Override
    public Outline infer(ThisNode node, Inferences inferences) {
        return findEntity(node);
    }

    private Outline findEntity(Node node) {
        AstScope scope = node.ast().symbolEnv().current();
        while(scope.scopeType()!= SCOPE_TYPE.IN_PRODUCT_ADT){
            scope = scope.parent();
            if(scope==null){
                GCPErrorReporter.report(node, GCPErrCode.UNAVAILABLE_THIS);
            }
        }
        return scope.node().outline();
//        while (node != null && !(node.outline() instanceof ProductADT)) {
//            node = node.parent();
//        }
//        if (node == null) {
//            return Outline.Unknown;
//        } else {
//            return node.outline();
//        }
    }
}
