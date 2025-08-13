package org.twelve.gcp.outlineenv;

import lombok.Setter;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.adt.ProductADT;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AstScope implements Scope {
    private final Long scopeId;
    private final Node node;
    private Map<String, EnvSymbol> symbols = new HashMap<>();
    private Map<String, EnvSymbol> outlines = new HashMap<>();
    private final AstScope parent;
    @Setter
    private SCOPE_TYPE scopeType = SCOPE_TYPE.IN_BLOCK;

    AstScope(Long scopeId, AstScope parent, Node node) {
        this.scopeId = scopeId;
        this.parent = parent;
        this.node = node;
    }

    public Long id() {
        return this.scopeId;
    }

    public Node node() {
        return this.node;
    }

//    public AstScope addScope(Long scopeId) {
//        return new AstScope(scopeId, this);
//    }

    public AstScope parent() {
        return this.parent;
    }


    public EnvSymbol defineSymbol(String key, Outline outline, boolean mutable, Identifier originNode) {
        if (this.symbols.containsKey(key)) {
            return this.symbols.get(key);
        }
        EnvSymbol symbol = new EnvSymbol(key, mutable, outline, this.scopeId, originNode);
        this.symbols.put(key, symbol);
        return symbol;
    }

    public EnvSymbol defineOutline(String key, Outline outline, Identifier originNode) {
        if (this.outlines.containsKey(key)) {
            return this.outlines.get(key);
        }
        EnvSymbol type = new EnvSymbol(key, outline, this.scopeId, originNode);
        this.outlines.put(key, type);
        return type;
    }

    public SCOPE_TYPE scopeType() {
        return this.scopeType;
    }

    public EnvSymbol lookupSymbol(String key, boolean isEntity) {
        EnvSymbol symbol = symbols.get(key);
        if (isEntity && this.scopeType == SCOPE_TYPE.IN_PRODUCT_ADT) {
            //find base
            EnvSymbol baseSymbol = this.lookupSymbol("base");
            if (baseSymbol != null) {
                Optional<EntityMember> member = ((ProductADT) baseSymbol.outline()).getMember(key);
                if (member.isPresent()) {
                    if (symbol == null) {
                        EntityMember m = member.get();
                        return new EnvSymbol(key, m.mutable().toBool(), m.outline(), m.node().scope(), m.node());
                    } else {
                        Poly poly = Poly.create(this.node().ast());
                        poly.sum(symbol.outline(), symbol.mutable());
                        poly.sum(member.get().outline(), member.get().mutable().toBool());
                        return new EnvSymbol(key, symbol.mutable(), poly, symbol.node().scope(), symbol.node());
                    }
                }
            }
        }
        return symbol;
    }

    public EnvSymbol lookupSymbol(String key) {
        return symbols.get(key);
    }

    public EnvSymbol lookupOutline(String key) {
        return outlines.get(key);
    }
}
