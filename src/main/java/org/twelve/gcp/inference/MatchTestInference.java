package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.node.expression.conditions.MatchTest;
import org.twelve.gcp.node.unpack.UnpackNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.BOOL;
import org.twelve.gcp.outline.projectable.Generic;

import static org.twelve.gcp.common.Tool.cast;

public class MatchTestInference implements Inference<MatchTest> {
    @Override
    public Outline infer(MatchTest node, Inferencer inferencer) {
        Outline subject = node.subject().infer(inferencer);//match a{}, this is outline of a
        Expression pattern = node.pattern();//match a{ b ->}, this is outline of b. b could be literal, id, unpack
        Outline outline = pattern.infer(inferencer);
//        if(tryGeneric(subject,outline)){
//            if (pattern instanceof UnpackNode) {
//                for (Identifier id : ((UnpackNode)pattern).identifiers()) {
//                    node.ast().symbolEnv().defineSymbol(id.name(), Generic.from(id,null), false, id);
//                }
//            }
//            return node.ast().Boolean;
//        }
        if (pattern instanceof LiteralNode<?>) {
            if (!outline.is(subject)) {
                GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, "expected outline is " + subject + ", but now is " + outline);
            }
            return node.ast().Boolean;
        }
        if (pattern instanceof Identifier) {
            node.ast().symbolEnv().defineSymbol(((Identifier) pattern).name(), subject, false, cast(pattern));
        }
        if (pattern instanceof UnpackNode) {
            ((UnpackNode)pattern).assign(node.ast().symbolEnv(),subject);
            if(subject instanceof Generic){
                ((Generic) subject).addHasToBe(pattern.infer(inferencer));
            }
        }
        pattern.infer(inferencer);
        if(node.condition()!=null) {
            Outline condition = node.condition().infer(inferencer);
            if(!(condition instanceof BOOL)){
                GCPErrorReporter.report(node,GCPErrCode.OUTLINE_MISMATCH,"match condition should be boolean");
            }
        }
        return node.ast().Boolean;
    }

    private boolean tryGeneric(Outline target, Outline pattern){
        if(!(target instanceof Generic)) return false;
        if(pattern instanceof UNKNOWN) return true;
        Generic generic = cast(target);
        if(generic.hasToBe() instanceof ANY) generic.addHasToBe(pattern);
        return true;
    }
}
