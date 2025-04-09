package org.twelve.gcp.inference;

import org.twelve.gcp.node.statement.ExpressionStatement;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;

public class ExprStatementInference  implements Inference<ExpressionStatement> {
    @Override
    public Outline infer(ExpressionStatement node, Inferences inferences) {
        return ProductADT.Ignore;
    }
}