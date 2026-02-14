package org.twelve.gcp.inference;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.namespace.NamespaceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Module;

/**
 * add imported symbol into local symbol environment from global environment
 */
public class ImportInference implements Inference<Import> {
    @Override
    public Outline infer(Import node, Inferences inferences) {
        String namespace = node.ast().namespace().lexeme();
        NamespaceNode ns = node.source().namespace();
        if(ns!=null) {//import source is in different namespace
            namespace = ns.lexeme();
        }
        Identifier moduleSymbol = node.source().name();
        Module module = node.ast().asf().globalEnv().lookup(namespace, moduleSymbol);
        for (ImportSpecifier _import : node.specifiers()) {
            if(_import.imported().lexeme().equals(CONSTANTS.STAR)){
                node.ast().symbolEnv().defineSymbol(moduleSymbol.name(), module, false, moduleSymbol);
            }else{
                Outline outline = module.lookup(_import.imported());
                node.ast().symbolEnv().defineSymbol(_import.local().name(),outline,false,moduleSymbol);
            }
            _import.infer(inferences);
        }
        return node.ast().Ignore;
    }
}
