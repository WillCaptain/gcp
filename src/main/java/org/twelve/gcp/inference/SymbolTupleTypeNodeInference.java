package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.SymbolTupleTypeTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Tuple;
import org.twelve.gcp.outline.adt.SymbolTuple;
import org.twelve.gcp.outline.decorators.Lazy;
import org.twelve.gcp.outline.primitive.SYMBOL;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;

public class SymbolTupleTypeNodeInference implements Inference<SymbolTupleTypeTypeNode> {
    @Override
    public Outline infer(SymbolTupleTypeTypeNode node, Inferencer inferencer) {
        if (node.members().isEmpty()) {
            if (inferencer.isLazy()) {
                LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
//                EnvSymbol supposed = oEnv.lookupSymbol(node.symbol().name());
                EnvSymbol supposed = oEnv.lookupOutline(node.symbol().name());
                if (supposed == null) {//only when type is not there, lazy it
                    return new Lazy(node.symbol(), inferencer);
                } else {
                    return supposed.outline();
                }
            }
            return node.symbol().infer(inferencer);
        } else {
            Tuple tuple = cast(new TupleTypeNodeInference().infer(node, inferencer));
            SYMBOL symbol = cast(node.symbol().infer(inferencer));
            return new SymbolTuple(symbol, tuple);
        }
    }
}
