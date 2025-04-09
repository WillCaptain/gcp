package org.twelve.gcp.outlineenv;

import org.twelve.gcp.outline.builtin.Module;

import java.util.HashMap;
import java.util.Map;

public class GlobalScope implements Scope{
    private final long scopeId;
    private final GlobalSymbolEnvironment env;
    private final GlobalScope parent;
    private final GlobalScopeCategory category;
    private Map<String,GlobalScope> scopes = new HashMap<>();
    private Module module = null;
    private final String name;

    GlobalScope(String name, GlobalSymbolEnvironment env, GlobalScope parent, GlobalScopeCategory category) {
        this.scopeId = env.counter().getAndIncrement();
        this.env = env;
        this.parent = parent;
        this.category = category;
        this.name = name;
    }

    GlobalScope(String name, GlobalSymbolEnvironment env){
        this(name, env,null,GlobalScopeCategory.Root);
    }

    public GlobalScope addNamespace(String name) {
        return this.scopes.computeIfAbsent(name, k->new GlobalScope(name,env,this,GlobalScopeCategory.Namespace));
    }
    public void attachModule(Module module) {
        this.module =module;
    }

    public long scopeId(){
        return this.scopeId;
    }

    public GlobalScope getNamespace(String name){
        return this.scopes.get(name);
    }

    public Module module(){
        return this.module;
    }

    @Override
    public String toString() {
        if(this.category==GlobalScopeCategory.Root){
            return "global environment with "+this.scopes.size()+" modules in";
        }
        StringBuilder sb = new StringBuilder();
        GlobalScope scope = this;
        while(scope.category!=GlobalScopeCategory.Root){
            sb.insert(0,"."+scope.name);
            scope = scope.parent;
        }
        return this.category.name().toLowerCase()+": "+sb.substring(1);
    }
}
