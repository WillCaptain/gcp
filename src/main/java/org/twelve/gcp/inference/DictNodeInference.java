package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.DictNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Dict;

import java.util.concurrent.atomic.AtomicReference;

public class DictNodeInference implements Inference<DictNode> {
    @Override
    public Outline infer(DictNode node, Inferences inferences) {
        if(node.isEmpty()){
            return new Dict(node,Outline.Nothing,Outline.Nothing);
        }
        AtomicReference<Outline> key = new AtomicReference<>();
        AtomicReference<Outline> value = new AtomicReference<>();
        node.values().forEach((k,v)->{
            Outline inferredK = k.infer(inferences);
            if(key.get()==null){
                key.set(inferredK);
            }else{
                if(key.get().is(inferredK)){
                    key.set(inferredK);
                }
                if(!inferredK.is(key.get())){
                    ErrorReporter.report(k, GCPErrCode.OUTLINE_MISMATCH);
                }
            }
            Outline inferredV = v.infer(inferences);
            if(value.get()==null){
                value.set(inferredV);
            }else{
                if(value.get().is(inferredV)){
                    value.set(inferredV);
                }
                if(!inferredV.is(value.get())){
                    ErrorReporter.report(v, GCPErrCode.OUTLINE_MISMATCH);
                }
            }
        });
        return new Dict(node,key.get(),value.get());
    }
}
