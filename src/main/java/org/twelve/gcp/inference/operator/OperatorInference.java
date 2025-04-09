package org.twelve.gcp.inference.operator;

import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;

public interface OperatorInference {
    Outline infer(Outline left, Outline right, BinaryExpression node);
}
