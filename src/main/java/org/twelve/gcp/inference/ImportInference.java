package org.twelve.gcp.inference;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.namespace.NamespaceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.builtin.Module;
import org.twelve.gcp.outline.decorators.LazyModuleSymbol;
import org.twelve.gcp.outlineenv.EnvSymbol;

/**
 * Injects imported symbols from the global environment into the local symbol scope.
 * <p>
 * Supports <em>mutual module imports</em> (A ↔ B) via
 * {@link Module#lazyLookup}: if the source module has not yet exported a
 * requested symbol, a {@link LazyModuleSymbol} placeholder is stored instead of
 * failing immediately.  The placeholder's {@code inferred()} returns {@code false}
 * until the source module exports the symbol, which prevents the import node from
 * being marked {@code fullyInferred} and forces re-evaluation in the next inference
 * round.
 * <p>
 * On subsequent rounds the placeholder resolves to the concrete type; the already-
 * registered {@link EnvSymbol} is updated in-place via {@link EnvSymbol#update} so
 * that downstream nodes pick up the real type without needing to be invalidated.
 */
public class ImportInference implements Inference<Import> {
    @Override
    public Outline infer(Import node, Inferencer inferencer) {
        String namespace = node.ast().namespace().lexeme();
        NamespaceNode ns = node.source().namespace();
        if (ns != null) { // import source is in a different namespace
            namespace = ns.lexeme();
        }
        Identifier moduleSymbol = node.source().name();
        Module module = node.ast().asf().globalEnv().lookup(namespace, moduleSymbol);
        if (module == null) {
            return node.ast().Ignore;
        }

        boolean isLastPass = node.ast().asf().isLastInfer();

        for (ImportSpecifier _import : node.specifiers()) {
            if (_import.imported().lexeme().equals(CONSTANTS.STAR)) {
                // Star import: register the whole Module object; new exports become visible
                // automatically because it is the same instance held in GlobalScope.
                node.ast().symbolEnv().defineSymbol(moduleSymbol.name(), module, false, moduleSymbol);
            } else {
                // Named import: use lazyLookup so mutual-import cycles don't deadlock.
                Outline outline = module.lazyLookup(_import.imported(), isLastPass);
                if (outline == null) continue;

                String localName = _import.local().name();

                // Re-inference support: if the symbol was previously registered as a
                // LazyModuleSymbol placeholder, update it in-place with the resolved type.
                EnvSymbol existing = node.ast().symbolEnv().lookupSymbol(localName);
                if (existing != null) {
                    existing.update(outline);
                } else {
                    node.ast().symbolEnv().defineSymbol(localName, outline, false, moduleSymbol);
                }

                // Register concrete Entity types in the outline namespace so that they
                // can be used in type-annotation positions (e.g. "let x: TypeB = ...").
                // LazyModuleSymbol is intentionally excluded here; the registration will
                // happen in the next round once the symbol resolves to an Entity.
                if (outline instanceof Entity) {
                    node.ast().symbolEnv().defineOutline(localName, outline, moduleSymbol);
                }
            }
            _import.infer(inferencer);
        }
        return node.ast().Ignore;
    }
}
