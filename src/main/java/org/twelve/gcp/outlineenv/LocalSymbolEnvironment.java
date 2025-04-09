package org.twelve.gcp.outlineenv;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Module;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.twelve.gcp.common.Tool.cast;

public class LocalSymbolEnvironment implements SymbolEnvironment {
    private final AstScope root;
    private final Map<Long, AstScope> scopes = new HashMap<>();
    private AstScope current;
    private Module module = new Module();
    private Stack<AstScope> scopeStack = new Stack<>();

    public LocalSymbolEnvironment(Long scopeId) {
        this.root = new AstScope(scopeId, null);
        setCurrent(this.root);
    }

    private void setCurrent(AstScope scope) {
        this.scopeStack.push(scope);
        if (!this.scopes.containsKey(scope.id())) {
            this.scopes.put(scope.id(), scope);
        }
        this.current = scope;
//        this.previous = scope.parent();
    }

    public AstScope root() {
        return this.root;
    }

    public AstScope enter(Long scopeId) {
//        if(this.current.id()==scopeId){
//            this.previous = this.current;
//            return this.current;
//        }
        AstScope scope = this.scopes.get(scopeId);
        if (scope == null) {
            scope = this.current.addScope(scopeId);
        }
        setCurrent(scope);
        return this.current;
    }

    public AstScope exit() {
//        if (previous != null) {
//            this.current = previous;
//        }
        this.current = scopeStack.pop();
        return this.current;
    }

    public EnvSymbol lookup(String key) {
        EnvSymbol symbol = null;
        AstScope scope = this.current;
        while (symbol == null) {
            symbol = scope.lookup(key);
            if (scope.parent() instanceof AstScope) {
                scope = cast(scope.parent());
            } else {
                break;
            }
        }
        return symbol;
    }

    public EnvSymbol defineSymbol(String key, Outline outline, boolean mutable, Node originNode) {
       return this.current.defineSymbol(key, outline,mutable,false,originNode);
    }
    public EnvSymbol defineSymbol(String key, Outline outline,boolean mutable, boolean isDeclared, Node originNode) {
        return this.current.defineSymbol(key, outline,mutable,isDeclared,originNode);
    }

    public Module module() {
        return this.module;
    }

    public void exportSymbol(String name, Outline outline) {
        this.module.defineSymbol(name, outline);
    }

//    public void exportFunction(String name, Function outline) {
//        this.module.defineFunction(name, outline);
//    }
}
