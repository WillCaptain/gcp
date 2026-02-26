package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.DictNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Dict;

import java.util.concurrent.atomic.AtomicReference;

public class DictNodeInference implements Inference<DictNode> {
    @Override
    public Outline infer(DictNode node, Inferencer inferencer) {
        if(node.isEmpty()){
            return new Dict(node,node.ast().Nothing,node.ast().Nothing);
        }
        AtomicReference<Outline> key = new AtomicReference<>();
        AtomicReference<Outline> value = new AtomicReference<>();
        node.values().forEach((k,v)->{
            Outline inferredK = k.infer(inferencer);
            if(key.get()==null){
                key.set(inferredK);
            }else{
                if(key.get().is(inferredK)){
                    key.set(inferredK);
                }
                if(!inferredK.is(key.get())){
                    GCPErrorReporter.report(k, GCPErrCode.OUTLINE_MISMATCH);
                }
            }
            Outline inferredV = v.infer(inferencer);
            if(value.get()==null){
                value.set(inferredV);
            }else{
                if(value.get().is(inferredV)){
                    value.set(inferredV);
                }
                if(!inferredV.is(value.get())){
                    GCPErrorReporter.report(v, GCPErrCode.OUTLINE_MISMATCH);
                }
            }
        });
        return new Dict(node,key.get(),value.get());
    }
}
