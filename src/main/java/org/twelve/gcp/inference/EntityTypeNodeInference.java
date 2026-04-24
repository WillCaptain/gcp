package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.Literal;
import org.twelve.gcp.outline.projectable.Reference;

public class EntityTypeNodeInference implements Inference<EntityTypeNode> {
    @Override
    public Outline infer(EntityTypeNode node, Inferencer inferencer) {

        return this.infer(node, inferencer, node.ast().Any);
    }

    public Outline infer(EntityTypeNode node, Inferencer inferencer, Outline base) {
        node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
        Entity entity = Entity.fromRefs(node, node.refs().stream().map(r -> (Reference) r.infer(inferencer)).toList());
        node.ast().symbolEnv().current().setOutline(entity);

        Inferencer sessionInferencer = new OutlineInferencer(true);
        for (Variable m : node.members()) {
            Node defaultValueNode = node.getDefault(m.name());
            Outline declared;
            if (defaultValueNode != null) {
                // Infer the actual type of the default-value expression (lambda, string, entity, etc.)
                // so that function-typed fields (e.g. run: ()->this.speed) get a callable outline.
                Outline inferred = defaultValueNode.infer(inferencer);
                // Grammar: `entity_field : ID (':' (declared_outline | literal))?`
                //   `id:0`    → literal branch (bare value = default value, type should widen).
                //   `id:#1`   → declared_outline → literal_type branch (explicit literal type, keep).
                //   `id:0|3|4`→ declared_outline → option-of-literals (goes through else-branch below).
                // When the user writes a plain default-value literal (`id:0`, `name:"x"`), the
                // raw inference of that literal node is a `Literal` wrapper. Treating that as the
                // field's declared type would make `id` ≡ the singleton value 0. That's almost
                // never what the user wants here — they mean "type is Int, default is 0". Widen
                // the Literal to its origin primitive so downstream type inference (e.g. the
                // return type of `{..}->Entity` invocations, `.id` access returning Int) works.
                if (inferred instanceof Literal lit) {
                    inferred = lit.outline();
                }
                declared = (inferred instanceof UNKNOWN) ? node.ast().Any : inferred;
                entity.addMemberWithDefault(m.name(), declared, m.modifier(), m.mutable(), m, defaultValueNode);
            } else {
                declared = m.declared() == null ? node.ast().Any : m.declared().infer(sessionInferencer);
                // literal-type fields (e.g. issuer: #"GCP-System") are auto-filled — mark isDefault=true
                boolean isLiteralField = declared instanceof Literal;
                entity.addMember(m.name(), declared, m.modifier(), m.mutable(), m, isLiteralField);
            }
        }

        return entity;
    }
}
