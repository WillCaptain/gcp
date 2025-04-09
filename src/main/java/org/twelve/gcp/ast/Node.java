package org.twelve.gcp.ast;

import com.sun.xml.ws.developer.Serialization;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.Result;
import org.twelve.gcp.outline.Outline;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * node interface in outline ast
 */
@Serialization
public interface Node<A extends AST, N extends Node<A,N>> extends Serializable {

    /**
     * Unique identifier for the node in the AST.
     * Used for distinguishing nodes, especially when you may need to reference or manipulate specific nodes.
     *
     * @return Long - index of the node
     */
    @Serialization
    Long id();

    /**
     * Location in the source code where this node appears.
     * This can be helpful for debugging, error reporting, or source mapping.
     *
     * @return Location - represents the location (e.g., line/column number) of the node.
     */
    @Serialization
    Location loc();

    /**
     * Type of the node.
     * This could represent different kinds of nodes like literals, function definitions, variable declarations, etc.
     * Helps in identifying what the node represents within the AST.
     *
     * @return NodeType - enum describing the type of the node.
     */
    @Serialization
    String type();

    /**
     * Outline means type information for the node.
     * This reflects the type of the node in Outline’s type system.
     * For instance, a function node might have an outline like Number -> String.
     * A literal number might have an outline of Number, and so on.
     *
     * @return Outline - the type information for this node, which could be a custom type representing Outline's type system.
     */
    @Serialization
    Outline outline();

    Long scope();

    //<T extends Node> T createNode(NodeCreator<T> creator);
    String lexeme();

    /**
     * Get the child nodes of the current node.
     * Useful for top-down traversal or processing of the AST.
     *
     * @return List<Node> - list of child nodes.
     */
    List<N> nodes();

    default N get(int index) {
        return this.nodes().get(index);
    }

    A ast();

    <T extends N> T addNode(T node);
    <T extends N> T addNode(int index, T node);

    boolean removeNode(Node node);

    Node removeNode(int index);

    Map<String, Object> serialize();

    Outline infer(Inferences inference);

    <T> Result<T> interpret(Interpreter interpreter);

    Node parent();

    <T extends ONode> T replaceNode(N old, T now);

}
