package org.twelve.gcp.ast;

import lombok.Setter;
import org.twelve.gcp.common.Tool;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPRuntimeException;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.Result;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ERROR;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

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

public abstract class AbstractNode implements Node {
    // Structural Properties
    private final List<Node> nodes = new ArrayList<>();
    @Setter
    protected Node parent = null;
    private final Long id;  // Unique node identifier
    private final AST ast;  // Owning AST
    private final Location loc;  // Source location

    // Semantic Properties
    protected Outline outline;  // Type/scope information

    protected AbstractNode(AST ast, Location loc, Outline outline) {
        this.id = ast.nodeIndexer().getAndIncrement();
        this.ast = ast;
        this.loc = loc;
        this.outline = outline;
    }

    public AbstractNode(AST ast, Location loc) {
        this(ast, loc, null);
        this.outline = ast.unknown(this);
    }

    public AbstractNode(AST ast) {
        this(ast, null);
    }

    // --- Tree Structure Operations ---

   @Override
    public <T extends Node> T addNode(T node) {
        return this.addNode(this.nodes.size(), node);
    }

    @Override
    public <T extends Node> T addNode(int index, T node) {
        if (node == null) {
            return null;
        }
        if (this.ast != node.ast()) {
//            System.out.println(node.ast());
//            System.out.println(this.ast);
            GCPErrorReporter.report(GCPErrCode.NODE_AST_MISMATCH);
        }
        this.nodes.add(index, cast(node));
        node.setParent(this);
        return node;
    }

    @Override
    public <T extends Node> T replaceNode(Node old, T now) {
        int index = this.nodes.indexOf(old);
        this.nodes.remove(old);
        this.nodes.add(index, cast(now));
        now.setParent(this);
        old.setParent(null);
        return now;
    }

    @Override
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

    @Override
    public Outline infer(Inferences inferences) {
        try {
            if (!this.inferred()) {
                this.ast().symbolEnv().enter(this);
                try {
                    this.clearError();
                    this.outline = this.accept(inferences);
                } catch (Exception ex) {
                    if (this.ast().asf().isLastInfer()) {
                        throw ex;
                    } else {
                        this.outline = this.ast().unknown(this);
                    }
                }
                this.ast().symbolEnv().exit();
            }
        } catch (GCPRuntimeException e) {
            GCPErrorReporter.report(this, e.errCode(), "unexpected exception occurs in outline inference");
        }
        if (outline instanceof ERROR && !this.ast().asf().isLastInfer()) {
            this.outline = this.ast().unknown(this);
        }
        return outline;
    }
    @Override
    public void clearError() {
        this.ast().errors().removeIf(e -> e.node() == this);
//        for (Node node : this.nodes()) {
//            node.clearError();
//        }
    }

    @Override
    public Long parentScope() {
        Node parent = this;
        Long scope = this.scope();
        while (scope.equals(this.scope())) {
            parent = parent.parent();
            if (parent == null) break;
            scope = parent.scope();
        }
        return scope;
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public boolean inferred() {
        if (!this.outline.inferred()) {
            ast.missInferred().add(this);
            return false;
        }
        return this.nodes.stream().allMatch(Node::inferred);
    }

    @Override
    public void markUnknowns() {
        if (this.outline() instanceof UNKNOWN) {
            GCPErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
        this.nodes.forEach(Node::markUnknowns);
    }

    @Override
    public String lexeme() {
        return this.nodes().stream()
                .map(Node::lexeme)
                .collect(Collectors.joining("\n"));
    }
    @Override
    public Map<String, Object> serialize() {
        return Tool.serializeAnnotated(this);
    }

    @Override
    public Long id() {
        return id;
    }
    @Override
    public AST ast() {
        return ast;
    }
    @Override
    public Node parent() {
        return parent;
    }
    @Override
    public Outline outline() {
        return outline;
    }
    @Override
    public List<Node> nodes() {
//        return new ArrayList<>(nodes);
        return nodes;
    }
    @Override
    public Long scope() {
        return this.parent().scope();
    }
    @Override
    public String type() {
        return this.getClass().getSimpleName();
    }
    @Override
    public long nodeIndex() {
        return this.parent().nodes().indexOf(this);
    }
    @Override
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
     *
     * @param interpreter the interpreter for the node
     * @param <T>         result data type
     * @return result of the interpretation
     */
    public <T> Result<T> interpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }

    @Override
    public Node invalidate() {
        this.outline = this.ast().unknown(this);//ensure it is going to infer again
        return this;
    }
}
