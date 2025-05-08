package org.twelve.gcp.ast;

import org.twelve.gcp.common.Tool;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPRuntimeException;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.Result;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.Unknown;

/**
 * Abstract base class for all AST nodes in the compiler/interpreter pipeline.
 * <p>
 * Key Responsibilities:
 * - Tree structure management (parent/child relationships)
 * - Type inference via visitor pattern
 * - Source location tracking
 * - Error reporting and validation
 * <p>
 * Thread Safety: Not thread-safe for mutation operations (node addition/removal).
 *
 * @author huizi 2025
 */

public abstract class Node implements Serializable {
    // Structural Properties
    private final List<Node> nodes = new ArrayList<>();
    protected Node parent = null;
    private final Long id;  // Unique node identifier
    private final AST ast;  // Owning AST
    private final Location loc;  // Source location

    // Semantic Properties
    protected Outline outline;  // Type/scope information

    protected Node(AST ast, Location loc, Outline outline) {
        this.id = ast.nodeIndexer().getAndIncrement();
        this.ast = ast;
        this.loc = loc;
        this.outline = outline;
    }

    public Node(AST ast, Location loc) {
        this(ast, loc, Unknown);
    }

    public Node(AST ast) {
        this(ast, null);
    }

    // --- Tree Structure Operations ---

    /**
     * Adds a child node at the end of the child list.
     *
     * @throws GCPRuntimeException if node belongs to different AST
     */
    public <T extends Node> T addNode(T node) {
        return this.addNode(this.nodes.size(), node);
    }

    /**
     * Inserts a child node at specified position.
     *
     * @param index Insertion position
     * @param node  Node to add (null is allowed but returns null)
     */
    public <T extends Node> T addNode(int index, T node) {
        if (node == null) {
            return null;
        }
        if (this.ast != node.ast()) {
            System.out.println(node.ast());
            System.out.println(this.ast);
            ErrorReporter.report(GCPErrCode.NODE_AST_MISMATCH);
        }
        this.nodes.add(index, cast(node));
        node.parent = this;
        return node;
    }

    /**
     * Replaces a child node while preserving position.
     *
     * @return The new node
     */
    public <T extends Node> T replaceNode(Node old, T now) {
        int index = this.nodes.indexOf(old);
        this.nodes.remove(old);
        this.nodes.add(index, cast(now));
        now.parent = this;
        old.parent = null;
        return now;
    }

    /**
     * Gets the source location, falling back to children's span if not set.
     */
    public Location loc() {
        if (loc != null) return loc;

        List<Node> locatedNodes = nodes().stream()
                .filter(n -> !(n.loc().start() == 0 && n.loc().end() == 0))
                .toList();

        return locatedNodes.isEmpty() ?
                new SimpleLocation(0, 0) :
                new SimpleLocation(
                        locatedNodes.getFirst().loc().start(),
                        locatedNodes.getLast().loc().end());

    }

    // --- Type Inference ---

    /**
     * Performs type inference using visitor pattern.
     * Manages symbol table scope during inference.
     */
    public Outline infer(Inferences inferences) {
        try {
            if (!this.inferred()) {
                this.ast().symbolEnv().enter(this);
                try {
                    this.clearError();
                    this.outline = this.accept(inferences);
                }catch (Exception ex){
                    if(this.ast().asf().isLastInfer()){
                        throw ex;
                    }else {
                        this.outline = Unknown;
                    }
                }
                this.ast().symbolEnv().exit();
            }
        } catch (GCPRuntimeException e) {
            ErrorReporter.report(this, e.errCode(),"unexpected exception occurs in outline inference");
        }
        return outline;
    }

    public void clearError() {
        this.ast().errors().removeIf(e->e.node()==this);
//        for (Node node : this.nodes()) {
//            node.clearError();
//        }
    }

    public Long parentScope() {
        Node parent = this;
        Long scope=this.scope();
        while(scope.equals(this.scope())){
            parent = parent.parent();
            if(parent==null) break;
            scope = parent.scope();
        }
        return scope;
    }

    /**
     * Visitor pattern entry point for inference.
     */
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    // --- Validation ---
    /**
     * Recursively checks if node and all children are fully inferred.
     */
    public boolean inferred() {
        if (!this.outline.inferred()) return false;
        return this.nodes.stream().allMatch(Node::inferred);
    }

    /**
     * Marks unresolved nodes with inference errors.
     */
    public void markUnknowns() {
        if (this.outline()  instanceof UNKNOWN) {
            ErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
        this.nodes.forEach(Node::markUnknowns);
    }

    // --- Utility Methods ---
    public String lexeme() {
        return this.nodes().stream()
                .map(Node::lexeme)
                .collect(Collectors.joining("\n"));
    }

    public Map<String, Object> serialize() {
        return Tool.serializeAnnotated(this);
    }

    // --- Getters ---
    public Long id() { return id; }
    public AST ast() { return ast; }
    public Node parent() { return parent; }
    public Outline outline() { return outline; }
    public List<Node> nodes() {
//        return new ArrayList<>(nodes);
        return nodes;
    }
    public Long scope() {
        return this.parent().scope();
    }
    public String type() {
        return this.getClass().getSimpleName();
    }

    public int index() {
        return this.parent().nodes().indexOf(this);
    }

    public Node get(int index) {
        return this.nodes().get(index);
    }

    @Override
    public String toString() {
        return this.lexeme();
    }

    // --- Interpreter ---
    /**
     * runtime interpreter for the node
     * @param interpreter the interpreter for the node
     * @return result of the interpretation
     * @param <T> result data type
     */
    public <T> Result<T> interpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }

    public Node invalidate() {
        this.outline =Unknown;//ensure it is going to infer again
        return this;
    }
}
