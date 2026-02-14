package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;

public interface SymbolTypeNode<T extends EntityTypeNode> {
    SymbolIdentifier symbol();
    T data();
}
