package org.twelve.gcp.inference;

import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;

public class ExportInference implements Inference<Export> {
    @Override
    public Outline infer(Export node, Inferences inferences) {
        for (ExportSpecifier export : node.specifiers()) {
            Outline local = export.local().infer(inferences);
            node.ast().symbolEnv().exportSymbol(export.exported().name(), local);
            export.infer(inferences);
        }
        return node.ast().Ignore;
    }
}
