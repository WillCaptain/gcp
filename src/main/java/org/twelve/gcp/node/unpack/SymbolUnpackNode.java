package org.twelve.gcp.node.unpack;

import org.twelve.gcp.node.expression.SymbolIdentifier;

public interface SymbolUnpackNode<T extends UnpackNode> {
    SymbolIdentifier symbol();
    T unpackNode();
}
