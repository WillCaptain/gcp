package org.twelve.gcp.outlineenv;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.Outline;

import java.util.HashMap;
import java.util.Map;

public class AstScope implements Scope {
    private final Long scopeId;
    private Map<String, EnvSymbol> symbols = new HashMap<>();
    private final AstScope parent;

    AstScope(Long scopeId, AstScope parent) {
        this.scopeId = scopeId;
        this.parent = parent;
    }

    public Long id() {
        return this.scopeId;
    }

    public AstScope addScope(Long scopeId) {
        AstScope scope = new AstScope(scopeId, this);
        return scope;
    }

    public AstScope parent() {
        return this.parent;
    }


    public EnvSymbol defineSymbol(String key, Outline outline, boolean mutable, boolean isDeclared, ONode originNode) {
        if (this.symbols.containsKey(key)) {
            return this.symbols.get(key);
        }
        EnvSymbol symbol = new EnvSymbol(key, mutable, outline, isDeclared, this.scopeId, originNode);
        this.symbols.put(key, symbol);
        return symbol;
    }

    public EnvSymbol lookup(String key) {
        return symbols.get(key);
    }
}
