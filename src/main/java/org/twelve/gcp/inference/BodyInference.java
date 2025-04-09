package org.twelve.gcp.inference;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.body.Body;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;


/**
 * infer each statement
 * collect all return statement outline
 * if returns don't align inheritance chain return inference error
 * else return the base outline in the inheritance chain
 *
 * @param <T>
 */
public abstract class BodyInference<T extends Body> implements Inference<T> {
    @Override
    public Outline infer(T node, Inferences inferences) {
        Outline returns = null;
        for (int i = 0; i < node.nodes().size(); i++) {
            // Unreachable statement if we have more nodes left in the list
            if (returns != null) {
                //selection returns has no return branch, means it doesn't have full return, then the next statement is available
                if (!(returns instanceof Option && ((Option) returns).options().stream().anyMatch(o -> o == ProductADT.Ignore))) {
                    ErrorReporter.report(node.nodes().get(i), GCPErrCode.UNREACHABLE_STATEMENT);
                    return Option.Ignore;
                }
            }

            ONode child = node.nodes().get(i);
            Outline outline = child.infer(inferences);
            //all non ignore outline is return
            if (outline != ProductADT.Ignore) {
                if(returns ==null){
                    returns = outline;
                }else {
                    ((Option)returns).options().removeIf(o->o== ProductADT.Ignore);//remove the no return outline
                    returns = Option.from(node,returns,outline);
                }
            }
        }
        //didn't meet and return statement, then return Ignore
        if (returns == null) {
            returns = ProductADT.Ignore;
        }
        return returns;
    }
}
