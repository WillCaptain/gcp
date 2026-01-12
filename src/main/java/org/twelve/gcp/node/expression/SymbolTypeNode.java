package org.twelve.gcp.node.expression;

import org.twelve.gcp.node.expression.typeable.EntityTypeNode;

public interface SymbolTypeNode<T extends EntityTypeNode> {
    SymbolIdentifier symbol();
    T data();
}
