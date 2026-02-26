package org.twelve.gcp.inference;

import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.outline.Outline;

public class ExportInference implements Inference<Export> {
    @Override
    public Outline infer(Export node, Inferencer inferencer) {
        for (ExportSpecifier export : node.specifiers()) {
            Outline local = export.local().infer(inferencer);
            node.ast().symbolEnv().exportSymbol(export.exported().name(), local);
            export.infer(inferencer);
        }
        return node.ast().Ignore;
    }
}
