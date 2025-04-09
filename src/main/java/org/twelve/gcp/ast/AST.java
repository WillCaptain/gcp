package org.twelve.gcp.ast;

import org.twelve.gcp.exception.GCPError;
import org.twelve.gcp.node.namespace.NamespaceNode;
import org.twelve.gcp.outline.builtin.Module;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public interface AST<T extends Node> {
    T program();
    Long id();

    AtomicLong nodeIndexer();
    AtomicLong scopeIndexer();

    NamespaceNode namespace();

    String lexeme();

    String name();
    void addError(GCPError error);
    List<GCPError> errors();

    Module infer();
}
