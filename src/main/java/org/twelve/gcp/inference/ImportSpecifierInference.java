package org.twelve.gcp.inference;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.outline.Outline;

public class ImportSpecifierInference implements Inference<ImportSpecifier> {
    @Override
    public Outline infer(ImportSpecifier node, Inferences inferences) {
        Identifier moduleSymbol = ((Import)node.parent()).source().name();
        if(node.imported().lexeme().equals(CONSTANTS.STAR)){
            return node.ast().symbolEnv().lookupAll(moduleSymbol.token()).outline();
        }else{
            return node.ast().symbolEnv().lookupAll(node.local().token()).outline();
        }
    }

}
