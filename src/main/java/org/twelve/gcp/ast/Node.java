package org.twelve.gcp.ast;

import org.twelve.gcp.exception.GCPRuntimeException;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface Node extends Serializable {
    /**
     * Adds a child node at the end of the child list.
     *
     * @throws GCPRuntimeException if node belongs to different AST
     */
    <T extends Node> T addNode(T node);

    /**
     * Inserts a child node at specified position.
     *
     * @param index Insertion position
     * @param node  Node to add (null is allowed but returns null)
     */
    <T extends Node> T addNode(int index, T node);

    /**
     * Replaces a child node while preserving position.
     *
     * @return The new node
     */
    <T extends Node> T replaceNode(Node old, T now);

    /**
     * Gets the source location, falling back to children's span if not set.
     */
    Location loc();

    // --- Type Inference ---

    /**
     * Performs type inference using visitor pattern.
     * Manages symbol table scope during inference.
     */
    Outline infer(Inferences inferences);

    void clearError();

    Long parentScope();

    /**
     * Visitor pattern entry point for inference.
     */
    Outline accept(Inferences inferences);

    // --- Validation ---

    /**
     * Recursively checks if node and all children are fully inferred.
     */
    boolean inferred();

    /**
     * Marks unresolved nodes with inference errors.
     */
    void markUnknowns();

    // --- Utility Methods ---
    String lexeme();

    Map<String, Object> serialize();

    // --- Getters ---
    Long id();

    AST ast();

    Node parent();

    Outline outline();

    List<Node> nodes();

    Long scope();

    String type();

    long nodeIndex();

    Node get(int index);

    void setParent(Node parent);

    Node invalidate();

}
