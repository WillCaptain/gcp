package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;

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

//    @Override
//    public void assign(LocalSymbolEnvironment env, Outline inferred) {
//        if (!outline.is(inferred)) {
//            GCPErrorReporter.report(symbol, GCPErrCode.OUTLINE_MISMATCH, "expected outline is " + inferred + ", but now is " + outline);
//        }
//        super.assign(env, inferred);
//    }

    @Override
    public Outline accept(Inferences inferences) {
        this.outline = super.accept(inferences);
        return inferences.visit(this);
    }
}
