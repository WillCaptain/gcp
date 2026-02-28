package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.node.expression.typeable.LiteralTypeNode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.primitive.Literal;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityInterpretation implements Interpretation<EntityNode> {
    @Override
    public Value interpret(EntityNode node, Interpreter interp) {
        // Determine whether base refers to an outline entity template or a runtime ADT symbol
        EntityTypeNode templateType = resolveTemplate(node, interp);

        EntityValue baseVal = null;
        String symbolTag = null;

        if (templateType == null && node.base() != null) {
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
            // 1. Pre-fill default-value fields and literal-type fields from the outline template
            if (templateType != null) {
                for (Variable member : templateType.members()) {
                    String fieldName = member.name();
                    var defaultNode = templateType.getDefault(fieldName);
                    if (defaultNode != null) {
                        // field with explicit default value, e.g. alias: "alice"
                        Value defVal = interp.eval(defaultNode);
                        fields.put(fieldName, defVal);
                        entityEnv.define(fieldName, defVal);
                    } else if (member.declared() instanceof LiteralTypeNode ltn) {
                        // literal-type constant field, e.g. issuer: #"GCP-System"
                        var litOutline = ltn.outline();
                        if (litOutline instanceof Literal lit && lit.node() instanceof LiteralNode<?> ln) {
                            Value litVal = interp.eval(ln);
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
                if (templateType != null && isLiteralTypeField(templateType, fieldName)) continue;
                Value v = interp.eval(entry.getValue().expression());
                fields.put(fieldName, v);
                entityEnv.define(fieldName, v);
            }
        } finally {
            interp.setEnv(saved);
        }
        return self;
    }

    /** Returns true if the named field in the template is declared with a literal type (e.g. {@code issuer: #"GCP-System"}). */
    private static boolean isLiteralTypeField(EntityTypeNode templateType, String fieldName) {
        return templateType.members().stream()
                .anyMatch(m -> m.name().equals(fieldName) && m.declared() instanceof LiteralTypeNode);
    }

    /**
     * If the entity node's base is a name referring to an outline entity-type definition,
     * return that definition's {@link EntityTypeNode}; otherwise return {@code null}.
     */
    private static EntityTypeNode resolveTemplate(EntityNode node, Interpreter interp) {
        if (node.base() == null) return null;
        if (!(node.base() instanceof Identifier id)) return null;
        OutlineDefinition def = interp.typeDefinitions().get(id.name());
        if (def != null && def.typeNode() instanceof EntityTypeNode etn) return etn;
        return null;
    }
}
