package org.twelve.gcp.node.expression;

import lombok.Getter;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.SimpleLocation;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ERROR;
import org.twelve.gcp.outline.builtin.NOTHING;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class Variable  extends Identifier{
    private final Identifier name;
    @Getter
    private final Expression declared;
    private final Boolean mutable;

    public Variable(Identifier name,Boolean mutable,Expression declared) {
        super(declared.ast(),name.token());
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
        return name.name()+(declared.outline() instanceof UNKNOWN ?"":(": "+declared.outline()));
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
        if(this.declared.outline() instanceof UNKNOWN){
            return this.outline;
        }else{
            return this.declared.outline();
        }
    }

    public boolean mutable() {
        return this.mutable;
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if(!(this.outline instanceof ERROR)) {
            super.assign(env, inferred);
        }
    }
}
