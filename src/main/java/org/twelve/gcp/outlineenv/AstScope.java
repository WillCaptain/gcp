package org.twelve.gcp.outlineenv;

import lombok.Setter;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.SCOPE_TYPE;
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


    public EnvSymbol defineSymbol(String key, Outline outline, boolean mutable, boolean isDeclared, Node originNode) {
        if (this.symbols.containsKey(key)) {
            return this.symbols.get(key);
        }
        EnvSymbol symbol = new EnvSymbol(key, mutable, outline, isDeclared, this.scopeId, originNode);
        this.symbols.put(key, symbol);
        return symbol;
    }

    public SCOPE_TYPE scopeType() {
        return this.scopeType;
    }

    public EnvSymbol lookup(String key, boolean isEntity) {
        EnvSymbol symbol = symbols.get(key);
        if (isEntity && this.scopeType == SCOPE_TYPE.IN_PRODUCT_ADT) {
            //find base
            EnvSymbol baseSymbol = this.lookup("base");
            if (baseSymbol != null) {
                Optional<EntityMember> member = ((ProductADT) baseSymbol.outline()).getMember(key);
                if (member.isPresent()) {
                    if (symbol == null) {
                        EntityMember m = member.get();
                        return new EnvSymbol(key, m.mutable().toBool(), m.outline(), true, m.node().scope(), m.node());
                    } else {
                        Poly poly = Poly.create();
                        poly.sum(symbol.outline(),false);
                        poly.sum(member.get().outline(),false);
                        return new EnvSymbol(key, symbol.mutable(), poly, true, symbol.originNode().scope(), symbol.originNode());
                    }
                }
            }
        }
        return symbol;
    }

    public EnvSymbol lookup(String key) {
        return this.lookup(key, false);
    }
}
