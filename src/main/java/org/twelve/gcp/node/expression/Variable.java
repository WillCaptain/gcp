package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.SimpleLocation;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ERROR;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class Variable extends Identifier {
    private final Identifier identifier;
    private final TypeNode declared;
    private final Boolean mutable;

    public Variable(Identifier identifier, Boolean mutable, TypeNode declared) {
        super(identifier.ast(), identifier.token());
        this.mutable = mutable;
        this.identifier = identifier;
        this.declared = this.addNode(declared);
    }

    @Override
    public Location loc() {
        if (this.declared == null) {
            return this.identifier.loc();
        } else {
            return new SimpleLocation(this.identifier.loc().start(), this.declared.loc().end());
        }
    }

    public Identifier identifier() {
        return this.identifier;
    }

    @Override
    public String lexeme() {
        String ext = "";
        if (this.declared != null) {
            ext = ": " + this.declared.lexeme();
//            if(this.declared instanceof IdentifierType){
//                ext += this.declared.lexeme();
//            }else{
//                ext += this.declared.outline();
//            }
            if (ext.trim().equals(":")) ext = "";
        }
        return identifier.name() + ext;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

//    public Boolean isDeclared() {
//        return !(this.declared.outline() instanceof UNKNOWN);
//    }

    @Override
    public Outline outline() {
        if (this.declared == null || this.declared.outline().containsUnknown()) {
            return this.outline;
        } else {
            return this.declared.outline();
        }
    }

    public boolean mutable() {
        return this.mutable;
    }

    public TypeNode declared() {
        return this.declared;
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if (!(this.outline instanceof ERROR)) {
            super.assign(env, inferred);
        }
    }

    @Override
    protected EnvSymbol lookupSymbol(LocalSymbolEnvironment env, String name) {
        return env.current().lookupSymbol(name);
    }
}
