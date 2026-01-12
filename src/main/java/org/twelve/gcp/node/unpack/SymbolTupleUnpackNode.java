package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.SYMBOL;
import org.twelve.gcp.outline.unpack.Unpack;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class SymbolTupleUnpackNode extends TupleUnpackNode implements SymbolUnpackNode<TupleUnpackNode> {
    private final SymbolIdentifier symbol;

    public SymbolTupleUnpackNode(AST ast, SymbolIdentifier symbol, TupleUnpackNode tupleUnpackNode) {
        super(ast, tupleUnpackNode.begins, tupleUnpackNode.ends);
        this.symbol = symbol;
        this.outline = new Unpack(this, new SYMBOL(symbol.name(), ast));
    }

    @Override
    public SymbolIdentifier symbol() {
        return this.symbol;
    }

    @Override
    public TupleUnpackNode unpackNode() {
        return this;
    }

    @Override
    public String toString() {
        return this.symbol.toString() + super.toString();
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if (!outline.is(inferred)) {
            GCPErrorReporter.report(symbol, GCPErrCode.OUTLINE_MISMATCH, "expected outline is " + inferred + ", but now is " + outline);
        }
        super.assign(env, inferred);
    }
}