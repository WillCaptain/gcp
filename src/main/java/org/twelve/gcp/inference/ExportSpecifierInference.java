package org.twelve.gcp.inference;

import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.outline.Outline;

public class ExportSpecifierInference implements Inference<ExportSpecifier>{
    @Override
    public Outline infer(ExportSpecifier node, Inferences inferences) {
        return node.local().outline();
    }
}
