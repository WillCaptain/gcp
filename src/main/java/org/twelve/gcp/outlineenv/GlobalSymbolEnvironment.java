package org.twelve.gcp.outlineenv;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.builtin.Module;

import java.util.concurrent.atomic.AtomicLong;

public class GlobalSymbolEnvironment implements SymbolEnvironment {
    private AtomicLong counter = new AtomicLong(0);
    private GlobalScope root;
    private Scope current;

    public GlobalSymbolEnvironment() {
        this.root = new GlobalScope("root",this);
    }


    public GlobalScope createNamespace(String namespace) {
        String[] names = namespace.split("\\.");
        GlobalScope scope = root;
        for (String name : names) {
            scope = scope.addNamespace(name);
        }
        return scope;
    }

    public AtomicLong counter() {
        return this.counter;
    }

    public Module lookup(String namespace, Identifier moduleSymbol) {
        GlobalScope scope = root;
        for (String name : namespace.split("\\.")) {
            scope = scope.getNamespace(name);
        }
        scope = scope.getNamespace(moduleSymbol.name());//last level is the module level
        Module module = scope.module();
        if(module==null){
            GCPErrorReporter.report(moduleSymbol, GCPErrCode.MODULE_NOT_DEFINED);
            return null;
        }else{
            return module;
        }
    }
}
