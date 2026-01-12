package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.SYMBOL;
import org.twelve.gcp.outline.unpack.Unpack;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class SymbolEntityUnpackNode extends EntityUnpackNode implements SymbolUnpackNode<EntityUnpackNode> {
    private final SymbolIdentifier symbol;

    public SymbolEntityUnpackNode(AST ast, SymbolIdentifier symbol,EntityUnpackNode entityUnpackNode) {
        super(ast);
        this.symbol = symbol;
        this.outline = new Unpack(this, new SYMBOL(symbol.name(), ast));
        this.fields = entityUnpackNode.fields;
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
        if (!outline.is(inferred)) {
            GCPErrorReporter.report(symbol, GCPErrCode.OUTLINE_MISMATCH, "expected outline is " + inferred + ", but now is " + outline);
        }
        super.assign(env, inferred);
    }
}
