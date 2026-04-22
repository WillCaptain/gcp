package org.twelve.gcp.inference;

import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.decorators.This;
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
        // Qualified-ctor form:  Owner.Variant{...}
        // The base is a MemberAccessor whose member is a SymbolIdentifier (the tag).
        // Semantics: structural upcast — the user is asserting the literal "belongs to Owner"
        // as the specific Variant; result type is Owner (the SumADT). Row polymorphism is
        // honoured: extras allowed, required fields of the variant must all match.
        if (node.outline() instanceof UNKNOWN
                && node.base() instanceof MemberAccessor ma
                && ma.member() instanceof SymbolIdentifier tag) {
            return inferQualifiedCtor(node, ma, tag, inferencer);
        }
        if (node.outline() instanceof UNKNOWN) { // first inference
            // Resolve the base BEFORE setting IN_PRODUCT_ADT on the current scope.
            // If base is 'this', ThisInference traverses upward looking for IN_PRODUCT_ADT.
            // Setting our scope to IN_PRODUCT_ADT first (before the base is set) would make
            // ThisInference stop at THIS (empty) scope instead of the correct outer entity scope.
            Outline base = null;
            if (node.base() != null) {
                base = node.base().infer(inferencer);
                if (!(base instanceof Generic)) {
                    if (!(base instanceof ADT)) {
                        GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
                        return node.ast().Error;
                    }
                }
                // For this{...} updates, store the underlying entity (not the This wrapper)
                // so that AstScope.lookupSymbol can resolve bare field names via the member-lookup
                // mechanism (which expects base.outline() instanceof ADT, not This).
                Outline baseForScope = (base instanceof This t) ? t.eventual() : base;
                node.ast().symbolEnv().defineSymbol("base", baseForScope, false, null);
            }

            // ── this{field=expr, ...} ─────────────────────────────────────────────────
            // The static type of  this{...}  is This(outerEntity) — the same type as 'this'.
            // Field overrides are purely a runtime operation; they do NOT change the compile-time
            // type.  Creating a new child entity here would introduce circular references through
            // the updateThis / copy machinery, causing StackOverflows.
            // We still infer each override expression for error-detection, using the outer
            // entity as the ambient scope so that field names and generic parameters resolve.
            // The "base" symbol already points to the underlying entity (see above), enabling
            // AstScope.lookupSymbol to resolve bare field names like `x` in `this{x = x + dx}`.
            if (base instanceof This thisWrapper) {
                node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
                node.ast().symbolEnv().current().setOutline(thisWrapper.eventual());
                node.members().forEach((k, v) -> v.infer(inferencer));
                return base; // This(outerEntity)
            }
            // ─────────────────────────────────────────────────────────────────────────

            // Set scope type AFTER base resolution so that member inference (below) can
            // reference 'this' without confusing the scope lookup.
            node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);

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
            node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
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
                // For template construction, construction-provided data fields must replace (not
                // merge with) the template's typed placeholders (e.g. data:[i] → data:[Integer]).
                // Using replaceMember ensures the template member is removed before the concrete
                // value is added, preventing spurious Poly unions.
                if (baseTemplateOutline instanceof ProductADT && entity.getMember(k).isPresent()) {
                    entity.replaceMember(k, outline);
                } else {
                    entity.addMember(k, outline, v.modifier(), v.mutable(), v.identifier());
                }
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

    /**
     * Handle qualified constructor  {@code Owner.Variant{...}}.
     * Structural upcast: the literal must structurally satisfy {@code Variant} (row-polymorphism —
     * all required fields of the variant present and compatible; extras allowed). The result type
     * is {@code Owner} (the enclosing {@link SumADT}).
     */
    private Outline inferQualifiedCtor(EntityNode node, MemberAccessor ma, SymbolIdentifier tag,
                                       Inferencer inferencer) {
        Outline hostOutline = ma.host().infer(inferencer);
        Outline sumish = hostOutline;
        if (sumish instanceof This t) sumish = t.eventual();
        if (!(sumish instanceof SumADT sumADT)) {
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH,
                    hostOutline + " is not a sum type; qualified constructor requires a named ADT");
            return node.ast().Error;
        }
        SymbolEntity variant = null;
        for (Outline opt : sumADT.options()) {
            if (opt instanceof SymbolEntity se
                    && se.base() instanceof SYMBOL sym
                    && sym.toString().equals(tag.name())) {
                variant = se;
                break;
            }
        }
        if (variant == null) {
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH,
                    "'" + tag.name() + "' is not a variant of " + hostOutline);
            return node.ast().Error;
        }
        // Build the proposed entity from user-provided members
        node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
        Entity proposed = Entity.from(node, new ArrayList<>());
        node.ast().symbolEnv().current().setOutline(proposed);
        // two-phase: non-function members first so function bodies can reference them
        node.members().forEach((k, v) -> {
            if (!(v.expression() instanceof FunctionNode)) {
                Outline o = v.infer(inferencer);
                proposed.addMember(k, o, v.modifier(), v.mutable(), v.identifier());
            }
        });
        node.members().forEach((k, v) -> {
            if (v.expression() instanceof FunctionNode) {
                Outline o = v.infer(inferencer);
                proposed.addMember(k, o, v.modifier(), v.mutable(), v.identifier());
            }
        });
        // Structural (row-polymorphism) check: proposed.is(variant).
        // Entity.is iterates variant's required members and requires proposed to have matching
        // compatible fields; extras in proposed are allowed — this is exactly width-subtyping.
        boolean ok = proposed.is(variant);
        if (!ok) {
            // Build a human-readable missing/mismatch diagnostic focused on belonging, not
            // "field not found on Owner".
            List<String> missing = new ArrayList<>();
            List<String> wrong = new ArrayList<>();
            for (EntityMember m : variant.members()) {
                if (m.isDefault()) continue;
                Optional<EntityMember> found = proposed.getMember(m.name());
                if (found.isEmpty()) {
                    missing.add("'" + m.name() + "'");
                } else if (!found.get().outline().is(m.outline())) {
                    wrong.add("'" + m.name() + "':" + found.get().outline()
                            + " (expected " + m.outline() + ")");
                }
            }
            StringBuilder reason = new StringBuilder();
            if (!missing.isEmpty()) reason.append("missing ").append(String.join(", ", missing));
            if (!wrong.isEmpty()) {
                if (reason.length() > 0) reason.append("; ");
                reason.append("type mismatch on ").append(String.join(", ", wrong));
            }
            if (reason.length() == 0) reason.append("structure does not satisfy the variant");
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH,
                    "literal does not belong to '" + tag.name() + "' of " + hostOutline
                            + ": " + reason);
        }
        // Return the concrete SymbolEntity built from the user's literal (preserving the tag and
        // any extras the user supplied — row polymorphism). Downstream `is`-checks will still
        // successfully upcast this to the owning SumADT; returning the SumADT directly breaks
        // call-site projection (the formal's per-variant structural projection expects an
        // Entity-shaped projection, not a union).
        return ok ? new SymbolEntity((SYMBOL) variant.base(), proposed) : node.ast().Error;
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
