package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.Literal;
import org.twelve.gcp.plugin.GCPBuilderPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in implementation of {@link GCPBuilderPlugin} for the {@code __external_builder__}
 * constructor.
 *
 * <p>Unlike plugin-JAR implementations, this class is instantiated directly inside
 * {@link OutlineInterpreter} and holds references to the interpreter's runtime state
 * ({@code typeDefinitions} and {@code eval}).  The loading mechanism differs from plugin
 * JARs, but the <em>contract</em> — implementing {@link GCPBuilderPlugin} — is identical,
 * so all built-in and external builders are treated uniformly by the constructor registry.
 *
 * <h2>Semantics</h2>
 * <pre>
 *   let p = __external_builder__&lt;Human&gt;;
 *   // → EntityValue with every field filled from outline definition:
 *   //   String  → ""
 *   //   Int     → 0
 *   //   #Male   → Male (literal symbol)
 *   //   default → evaluated default node
 * </pre>
 *
 * <p>When followed by {@code {field=value, …}} (entity-extension syntax), the
 * {@link org.twelve.gcp.interpreter.interpretation.EntityInterpretation} naturally
 * extends the returned default entity with the provided overrides.
 */
public class ExternalBuilderPlugin implements GCPBuilderPlugin {

    private final Interpreter interpreter;
    private final Map<String, OutlineDefinition> typeDefinitions;

    /**
     * @param interpreter     the owning interpreter — used to {@code eval} default / literal nodes
     * @param typeDefinitions the interpreter's live type-definition registry
     *                        (populated as {@code outline} declarations are executed)
     */
    public ExternalBuilderPlugin(Interpreter interpreter,
                                 Map<String, OutlineDefinition> typeDefinitions) {
        this.interpreter     = interpreter;
        this.typeDefinitions = typeDefinitions;
    }

    @Override
    public String id() {
        return "external_builder";
    }

    @Override
    public Value construct(String id, List<String> typeArgs, List<Value> valueArgs) {
        if (typeArgs.isEmpty()) return UnitValue.INSTANCE;
        return buildInstance(typeArgs.get(0));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Value buildInstance(String typeName) {
        OutlineDefinition def = typeDefinitions.get(typeName);
        if (def == null) return new EntityValue(Map.of());

        List<EntityTypeNode> templates = collectTemplates(def);
        if (templates.isEmpty()) return new EntityValue(Map.of());

        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        for (EntityTypeNode template : templates) {
            for (Variable member : template.members()) {
                String fieldName = member.name();
                Node defaultNode = template.getDefault(fieldName);
                if (defaultNode != null) {
                    fields.put(fieldName, interpreter.eval(defaultNode));
                } else if (member.declared() instanceof LiteralTypeNode ltn) {
                    Outline litOutline = ltn.outline();
                    if (litOutline instanceof Literal lit) {
                        fields.put(fieldName, interpreter.eval(lit.node()));
                    }
                } else {
                    fields.put(fieldName, defaultValueForType(member.declared()));
                }
            }
        }
        return new EntityValue(fields);
    }

    private Value defaultValueForType(TypeNode declared) {
        if (declared instanceof IdentifierTypeNode itn) {
            return switch (itn.name()) {
                case "Int", "Integer", "Long"        -> IntValue.ZERO;
                case "String"                        -> new StringValue("");
                case "Bool"                          -> BoolValue.FALSE;
                case "Float", "Double", "Number"     -> new FloatValue(0.0);
                default                              -> UnitValue.INSTANCE;
            };
        }
        return UnitValue.INSTANCE;
    }

    /** Collects all {@link EntityTypeNode}s in the inheritance chain, base-first. */
    private List<EntityTypeNode> collectTemplates(OutlineDefinition def) {
        List<EntityTypeNode> result = new ArrayList<>();
        collectFromDef(def, result);
        return result;
    }

    private void collectFromDef(OutlineDefinition def, List<EntityTypeNode> result) {
        if (def.typeNode() instanceof EntityTypeNode etn) {
            result.add(etn);
        } else if (def.typeNode() instanceof ExtendTypeNode ext) {
            TypeNode base = ext.refCall() != null ? ext.refCall().host() : ext.base();
            if (base != null) {
                OutlineDefinition parentDef = typeDefinitions.get(base.lexeme());
                if (parentDef != null) collectFromDef(parentDef, result);
            }
            result.add(ext.extension());
        }
    }
}
