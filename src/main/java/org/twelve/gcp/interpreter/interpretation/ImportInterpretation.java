package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.namespace.ModuleNode;

import java.util.Map;

public class ImportInterpretation implements Interpretation<Import> {
    @Override
    public Value interpret(Import node, Interpreter interp) {
        ModuleNode srcNode = node.source();
        String simpleModuleName = srcNode.name().name();
        String fullName = srcNode.namespace() != null
                ? srcNode.namespace().lexeme() + "." + simpleModuleName
                : simpleModuleName;

        Map<String, Map<String, Value>> allExports = interp.moduleExports();
        Map<String, Value> exports = allExports.getOrDefault(fullName, allExports.get(simpleModuleName));
        if (exports == null) return UnitValue.INSTANCE;

        for (ImportSpecifier specifier : node.specifiers()) {
            String importedName = specifier.imported().name();
            String localName    = specifier.local().name();
            if ("*".equals(importedName)) {
                exports.forEach(interp.env()::define);
            } else {
                Value v = exports.get(importedName);
                if (v != null) interp.env().define(localName, v);
            }
        }
        return UnitValue.INSTANCE;
    }
}
