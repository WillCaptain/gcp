package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.node.expression.typeable.ExtendTypeNode;
import org.twelve.gcp.node.expression.typeable.LiteralTypeNode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.primitive.Literal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntityInterpretation implements Interpretation<EntityNode> {
    @Override
    public Value interpret(EntityNode node, Interpreter interp) {
        // Collect all templates in the inheritance chain (base-first, extension-last).
        // An empty list means this is not an outline template construction.
        List<EntityTypeNode> templates = collectTemplates(node, interp);
        EntityTypeNode primaryTemplate = templates.isEmpty() ? null : templates.getLast();

        EntityValue baseVal = null;
        String symbolTag = null;

        if (primaryTemplate == null && node.base() != null) {
            // Not an outline template – evaluate base as a runtime value (ADT symbol / entity)
            Value b = interp.eval(node.base());
            if (b instanceof EntityValue ev) {
                if (ev.hasSymbol() && ev.ownFields().isEmpty()) {
                    symbolTag = ev.symbolTag();
                } else {
                    baseVal = ev;
                }
            }
        }

        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        EntityValue self = symbolTag != null
                ? EntityValue.sharedTagged(fields, null, symbolTag)
                : EntityValue.shared(fields, baseVal);

        Environment entityEnv = interp.env().child();
        entityEnv.define("this", self);
        if (baseVal != null) {
            entityEnv.define("base", baseVal);
            entityEnv.define("baseNode", baseVal);
        }

        Environment saved = interp.env();
        interp.setEnv(entityEnv);
        try {
            // 1. Pre-fill default-value fields and literal-type fields from all templates
            //    (ancestors first so that the most-derived definition wins on overlap).
            for (EntityTypeNode templateType : templates) {
                for (Variable member : templateType.members()) {
                    String fieldName = member.name();
                    var defaultNode = templateType.getDefault(fieldName);
                    if (defaultNode != null) {
                        // field with explicit default value, e.g. alias: "alice" or map: (mapper)->...
                        Value defVal = interp.eval(defaultNode);
                        fields.put(fieldName, defVal);
                        entityEnv.define(fieldName, defVal);
                    } else if (member.declared() instanceof LiteralTypeNode ltn) {
                        // literal-type constant field, e.g. issuer: #"GCP-System"
                        var litOutline = ltn.outline();
                        if (litOutline instanceof Literal lit) {
                            Value litVal = interp.eval(lit.node());
                            fields.put(fieldName, litVal);
                            entityEnv.define(fieldName, litVal);
                        }
                    }
                }
            }

            // 2. Evaluate explicitly provided fields (overrides template defaults, but NOT literal-type constants)
            for (Map.Entry<String, MemberNode> entry : node.members().entrySet()) {
                String fieldName = entry.getKey();
                // Literal-type fields (e.g. issuer: #"GCP-System") cannot be overridden — skip them.
                if (primaryTemplate != null && isLiteralTypeField(templates, fieldName)) continue;
                Value v = interp.eval(entry.getValue().expression());
                fields.put(fieldName, v);
                entityEnv.define(fieldName, v);
            }
        } finally {
            interp.setEnv(saved);
        }
        return self;
    }

    /** Returns true if the named field is declared with a literal type in ANY of the templates. */
    private static boolean isLiteralTypeField(List<EntityTypeNode> templates, String fieldName) {
        return templates.stream().anyMatch(t ->
                t.members().stream().anyMatch(m ->
                        m.name().equals(fieldName) && m.declared() instanceof LiteralTypeNode));
    }

    /**
     * Collects all {@link EntityTypeNode}s in the inheritance chain for the given entity node,
     * ordered base-first and extension-last.  Returns an empty list when the base is not an
     * outline template name (i.e. runtime entity construction, not typed template construction).
     */
    private static List<EntityTypeNode> collectTemplates(EntityNode node, Interpreter interp) {
        if (node.base() == null || !(node.base() instanceof Identifier id)) return List.of();
        OutlineDefinition def = interp.typeDefinitions().get(id.name());
        if (def == null) return List.of();
        List<EntityTypeNode> result = new ArrayList<>();
        collectFromDef(def, result, interp);
        return result;
    }

    private static void collectFromDef(OutlineDefinition def, List<EntityTypeNode> result, Interpreter interp) {
        if (def.typeNode() instanceof EntityTypeNode etn) {
            result.add(etn);
        } else if (def.typeNode() instanceof ExtendTypeNode ext) {
            // Resolve the parent definition first (depth-first, base before extension)
            String parentName = resolveParentName(ext);
            if (parentName != null) {
                OutlineDefinition parentDef = interp.typeDefinitions().get(parentName);
                if (parentDef != null) collectFromDef(parentDef, result, interp);
            }
            result.add(ext.extension());
        }
    }

    /** Extracts the outline name that an ExtendTypeNode's base references. */
    private static String resolveParentName(ExtendTypeNode ext) {
        TypeNode base = ext.refCall() != null ? ext.refCall().host() : ext.base();
        return base != null ? base.lexeme() : null;
    }
}
