package org.twelve.gcp.inference;

import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.Literal;
import org.twelve.gcp.outline.primitive.SYMBOL;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
import org.twelve.gcp.outline.projectable.Generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.twelve.gcp.common.Tool.cast;

public class EntityInference implements Inference<EntityNode> {
    @Override
    public Outline infer(EntityNode node, Inferencer inferencer) {
        Entity entity;
        node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
        if (node.outline() instanceof UNKNOWN) { // first inference
            Outline base = null;
            if (node.base() != null) {
                base = node.base().infer(inferencer);
                if (!(base instanceof Generic)) {
                    if (!(base instanceof ADT)) {
                        GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
                        return node.ast().Error;
                    }
                }
                node.ast().symbolEnv().defineSymbol("base", base, false, null);
            }

            if (base instanceof SYMBOL) {
                entity = new SymbolEntity(cast(base), Entity.from(node, new ArrayList<>()));
            } else if (base instanceof ProductADT templateEntity
                    && node.base() instanceof SymbolIdentifier) {
                // SymbolIdentifier base resolving to a ProductADT means the programmer wrote
                // OutlineName{...} – this is outline template construction.
                // Result: an ANONYMOUS structural entity (base=ANY, no symbol name).
                entity = Entity.from(node); // base = ANY
                // copy all template members, preserving default-value metadata
                for (EntityMember m : templateEntity.members()) {
                    if (m.outline() instanceof FirstOrderFunction fn && fn.getThis() != null) {
                        entity.addMember(m.name(), cast(fn.copy()), m.modifier(),
                                m.mutable() == Mutable.True, m.node());
                    } else {
                        entity.addMember(m.name(), m.outline(), m.modifier(),
                                m.mutable() == Mutable.True, m.node());
                    }
                }
            } else {
                // entity extension / inheritance (e.g. person{override=...})
                entity = Entity.from(node, cast(base), new ArrayList<>());
                if (base instanceof ProductADT prod) {
                    cloneMethodMembers(entity, prod.members());
                }
            }
        } else { // nth inference
            entity = cast(node.outline());
        }
        node.ast().symbolEnv().current().setOutline(entity);
        // For template construction (ApiKey{...}), look up the template entity to check
        // for literal-type fields that must not be overridden.
        Outline baseTemplateOutline = (node.base() instanceof SymbolIdentifier)
                ? node.base().outline() : null;
        // Two-phase member inference: infer non-function members first so their types are
        // available in the entity scope when function bodies (which may reference them as
        // bare names) are inferred in the second phase.  This eliminates VARIABLE_NOT_DEFINED
        // and FIELD_NOT_FOUND errors that otherwise arise from non-deterministic HashMap
        // iteration order putting function members ahead of their dependencies.
        node.members().forEach((k, v) -> {
            if (!(v.expression() instanceof FunctionNode)) {
                Outline outline = v.infer(inferencer);
                // Literal-type fields (issuer: #"GCP-System") cannot be overridden at construction.
                if (baseTemplateOutline instanceof ProductADT templateEnt) {
                    Optional<EntityMember> tm = templateEnt.getMember(k);
                    if (tm.isPresent() && tm.get().outline() instanceof Literal) {
                        GCPErrorReporter.report(v.identifier(), GCPErrCode.NOT_ASSIGNABLE);
                        return;
                    }
                }
                entity.addMember(k, outline, v.modifier(), v.mutable(), v.identifier());
            }
        });
        node.members().forEach((k, v) -> {
            if (v.expression() instanceof FunctionNode) {
                Outline outline = v.infer(inferencer);
                entity.addMember(k, outline, v.modifier(), v.mutable(), v.identifier());
            }
        });
        // Check required-field errors on every pass (clearError() clears them before each pass,
        // so they must be re-reported here to survive to the final inference state).
        // Multiple missing fields are combined into one error per construction (AST deduplicates
        // errors by node+code, so separate reports for the same node would be silently dropped).
        if (node.base() instanceof SymbolIdentifier) {
            Outline base = node.base().outline();
            if (base instanceof ProductADT templateEntity) {
                List<String> missing = new ArrayList<>();
                for (EntityMember m : templateEntity.members()) {
                    if (!m.isDefault() && !node.members().containsKey(m.name())) {
                        missing.add("'" + m.name() + "'");
                    }
                }
                if (!missing.isEmpty()) {
                    GCPErrorReporter.report(node, GCPErrCode.MISSING_REQUIRED_FIELD,
                            String.join(", ", missing) + " required but not provided");
                }
            }
        }
        return entity;
    }

    /** Clone {@link FirstOrderFunction} members that carry a {@code this} binding (for entity extension). */
    private void cloneMethodMembers(Entity entity, List<EntityMember> members) {
        List<EntityMember> ms = new ArrayList<>();
        members.forEach(m -> {
            if (m.outline() instanceof FirstOrderFunction fn && fn.getThis() != null) {
                FirstOrderFunction copy = cast(fn.copy());
                ms.add(EntityMember.from(m.name(), copy, m.modifier(), m.mutable() == Mutable.True, m.node(), m.isDefault()));
            }
        });
        entity.addMembers(ms);
    }
}
