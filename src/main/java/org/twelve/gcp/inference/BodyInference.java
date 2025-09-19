package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.body.Body;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.IGNORE;


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
            Node child = node.nodes().get(i);
            Outline outline = child.infer(inferences);
            //ignore IGNORE
            if (outline == node.ast().Ignore) {
                continue;
            }
            //first return
            if (returns == null) {
                returns = outline;
                continue;
            }
            //half return can have more return
            if(halfReturned(returns)){
                returns = Option.from(node, returns, outline);
            }else{
                break;
            }
            //meet full return, means no more return will be accepted
            if(!halfReturned(outline)){
                removeIgnore(returns);
                break;
            }
        }

        //didn't meet and return statement, then return Ignore
        if (returns == null) {
            returns = node.ast().Ignore;
        }
        return returns;
    }

    private void removeIgnore(Outline returns) {
        if(returns instanceof Option){
            ((Option) returns).options().removeIf(o->o instanceof IGNORE);
        }
    }

    private boolean halfReturned(Outline outline) {
        return outline instanceof Option && ((Option) outline).options()
                .stream().anyMatch(o -> o instanceof IGNORE);
    }
}
