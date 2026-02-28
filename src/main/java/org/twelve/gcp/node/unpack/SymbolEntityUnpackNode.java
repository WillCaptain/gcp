package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.SumADT;
import org.twelve.gcp.outline.adt.SymbolEntity;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class SymbolEntityUnpackNode extends EntityUnpackNode implements SymbolUnpackNode<EntityUnpackNode> {
    private final SymbolIdentifier symbol;

    public SymbolEntityUnpackNode(AST ast, SymbolIdentifier symbol,EntityUnpackNode entityUnpackNode) {
        super(ast);
        this.symbol = this.addNode(symbol);
        //this.outline = new Unpack(this, new SYMBOL(symbol));
        this.fields = entityUnpackNode.fields;
        for (Field field : this.fields) {
            if(field instanceof EntityField){
                this.addNode(field.field());
                if(((EntityField)field).as()!=null) {
                    this.addNode(((EntityField)field).as());
                }
            }
            if(field instanceof NestField){
                this.addNode(((NestField)field).nest());
            }
        }
    }

    @Override
    public SymbolIdentifier symbol() {
        return this.symbol;
    }

    @Override
    public String toString() {
        return this.symbol.toString() + super.toString();
    }

    @Override
    public EntityUnpackNode unpackNode() {
        return this;
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        // When the match subject is a union type (Option/Poly), narrow to the variant
        // whose symbol matches this pattern's symbol (e.g., Man{age as a} → Man variant).
        //
        // When the subject is a Generic with a declared type (e.g. person: Human),
        // the Generic itself is not a SumADT, but its declaredToBe constraint is.
        // Unwrap the declared type to find the correct variant.
        Outline effective = inferred;
        if (inferred instanceof Genericable<?, ?> g && !(inferred instanceof SumADT)) {
            Outline declared = g.declaredToBe();
            if (declared instanceof SumADT) {
                effective = declared;
            }
        }
        Outline resolved = inferred;
        if (effective instanceof SumADT sum) {
            resolved = sum.options().stream()
                    .filter(o -> o instanceof SymbolEntity se
                            && symbol.name().equals(se.base().toString()))
                    .findFirst()
                    .orElse(inferred);
        }
        super.assign(env, resolved);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        this.outline = super.acceptInfer(inferencer);
        return inferencer.visit(this);
    }
}
