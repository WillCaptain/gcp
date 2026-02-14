package org.twelve.gcp.outlineenv;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.builtin.Module;

import java.util.*;

public class LocalSymbolEnvironment implements SymbolEnvironment {
    private final AstScope root;
    private final Map<Long, AstScope> scopes = new HashMap<>();
    private final AST ast;
    private AstScope current;
    private final Module module;
    private Stack<AstScope> scopeStack = new Stack<>();

    public LocalSymbolEnvironment(AST ast) {
        this.ast = ast;
        this.module = new Module(ast);
        this.root = new AstScope(ast.program().scope(), null,ast.program());
        setCurrent(this.root);
        initOutlines(this.root);
    }

    public AST ast(){
        return this.ast;
    }

    private void initOutlines(AstScope root) {
        defineOutline(root,this.ast().String);
        defineOutline(root,this.ast().Integer);
        defineOutline(root,this.ast().Boolean);
        defineOutline(root,this.ast().Decimal);
        defineOutline(root,this.ast().Double);
        defineOutline(root,this.ast().Float);
        defineOutline(root,this.ast().Long);
        defineOutline(root,this.ast().Unit);
        defineOutline(root,this.ast().Number);
        defineOutline(root,this.ast().unknown());
    }
    private void defineOutline(AstScope root, Outline outline){
        root.defineOutline(outline.name(),outline,null);
    }

    private void setCurrent(AstScope scope) {
        this.scopeStack.push(scope);
        if (!this.scopes.containsKey(scope.id())) {
            this.scopes.put(scope.id(), scope);
        }
        this.current = scope;
    }

    public AstScope root() {
        return this.root;
    }

    public AstScope enter(AbstractNode node) {
        AstScope me = this.scopes.get(node.scope());
        AstScope parent = this.scopes.get(node.parentScope());
        if (me == null) {
            me = new AstScope(node.scope(),parent,node);
        }
        setCurrent(me);
        return this.current;
    }
    public AstScope enter(AstScope scope) {
        setCurrent(scope);
        return this.current;
    }

    public void exit() {
        scopeStack.pop();
        this.current = scopeStack.getLast();
    }

    public EnvSymbol lookupSymbol(String key){
        AstScope scope = this.current;
        boolean reachedThisScope = false;
        while(scope!=null){
            EnvSymbol symbol = scope.lookupSymbol(key, !reachedThisScope);
            if (symbol != null) return symbol;
            if(!reachedThisScope && scope.scopeType()== SCOPE_TYPE.IN_PRODUCT_ADT){
                reachedThisScope = true;
            }
            scope = scope.parent();
        }
        return null;
    }
    public EnvSymbol lookupAll(String key) {
        List<EnvSymbol> symbols = new ArrayList<>();
        AstScope scope = this.current;
        boolean reachedThisScope = false;//only possible to try to find base in the first product adt scope
        while(scope!=null){
            EnvSymbol symbol = scope.lookupSymbol(key, !reachedThisScope);
            if (symbol != null) symbols.add(symbol);
            if(!reachedThisScope && scope.scopeType()== SCOPE_TYPE.IN_PRODUCT_ADT){
                reachedThisScope = true;
            }
            scope = scope.parent();
        }

        if (symbols.isEmpty()) return null;
        if(symbols.size()==1) return symbols.getFirst();
        Outline outline = Poly.from(null,symbols.stream().map(EnvSymbol::outline).toArray(Outline[]::new));
        return new EnvSymbol(key, false, outline, this.current.id(), null);//null origin node, means it is merged
    }


    public EnvSymbol defineSymbol(String key, Outline outline, boolean mutable, Identifier originNode) {
        return this.current.defineSymbol(key, outline, mutable, originNode);
    }
    public void defineOutline(String key, Outline outline, Identifier originNode){
        this.current.defineOutline(key,outline,originNode);
    }

    public Module module() {
        return this.module;
    }

    public void exportSymbol(String name, Outline outline) {
        this.module.defineSymbol(name, outline);
    }

    public AstScope current() {
        return this.current;
    }

    public EnvSymbol lookupOutline(String key) {
        AstScope scope = this.current;
        while(scope!=null){
            EnvSymbol outline = scope.lookupOutline(key);
            if (outline != null) return outline;
            scope = scope.parent();
        }
        return null;
    }

//    public void exportFunction(String name, Function outline) {
//        this.module.defineFunction(name, outline);
//    }
}
