package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.SimpleLocation;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.typeable.TypeAble;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ERROR;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class Variable  extends Identifier{
    private final Identifier name;
    private final TypeAble declared;
    private final Boolean mutable;

    public Variable(Identifier name, Boolean mutable, TypeAble declared) {
        super(name.ast(),name.token());
        this.mutable = mutable;
        this.name = name;
        this.declared = declared;
    }

    @Override
    public Location loc() {
        if(this.declared==null){
            return this.name.loc();
        }else {
            return new SimpleLocation(this.name.loc().start(), this.declared.loc().end());
        }
    }
    @Override
    public String lexeme() {
        String ext = "";
        if(this.declared!=null){
            ext = ": ";
            if(this.declared instanceof Identifier){
                ext += this.declared.lexeme();
            }else{
                ext += this.declared.outline();
            }
        }
        return name.name()+ext;
    }
    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public Boolean isDeclared() {
        return !(this.declared.outline() instanceof UNKNOWN);
    }

    @Override
    public Outline outline() {
        if(this.declared==null || this.declared.outline() instanceof UNKNOWN){
            return this.outline;
        }else{
            return this.declared.outline();
        }
    }

    public boolean mutable() {
        return this.mutable;
    }
    public TypeAble declared() {
        return this.declared;
    }
    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if(!(this.outline instanceof ERROR)) {
            super.assign(env, inferred);
        }
    }
}
