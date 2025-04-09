package org.twelve.gcp.ast;

import org.twelve.gcp.common.Tool;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPRuntimeException;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.Result;
import org.twelve.gcp.outline.Outline;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.Unknown;

public abstract class Node implements Serializable {
    private final List<Node> nodes = new ArrayList<>();

    private final Long id;
    private final AST ast;
    private final Location loc;

    protected Outline outline;
    protected Node parent = null;

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

    public Long id() {
        return this.id;
    }

    public List<Node> nodes() {
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(this.nodes);
        return nodes;
    }

    public AST ast() {
        return this.ast;
    }

    public <T extends Node> T addNode(T node) {
        return this.addNode(this.nodes.size(), node);
    }

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

    public <T extends Node> T replaceNode(Node old, T now) {
        int index = this.nodes.indexOf(old);
        this.nodes.remove(old);
        this.nodes.add(index, cast(now));
        now.parent = this;
        old.parent = null;
        return now;
    }

    public boolean removeNode(Node node) {
        return this.nodes.remove(node);
    }

    public Node removeNode(int index) {
        return this.nodes.remove(index);
    }

    public Location loc() {
        if (loc != null) {
            return this.loc;
        } else {
            List<Node> nodes = nodes().stream()
                    .filter(n -> !(n.loc().start() == 0 && n.loc().end() == 0))
                    .collect(Collectors.toList());
            if (nodes.size() == 0) {
                return new SimpleLocation(0, 0);
            } else {
                return new SimpleLocation(nodes.get(0).loc().start(), nodes.get(nodes.size() - 1).loc().end());
            }
        }

    }

    public Node parent() {
        return this.parent;
    }

    public Long scope() {
        return this.parent().scope();
    }

    public String type() {
        return this.getClass().getSimpleName();
    }

    public Outline outline() {
        return this.outline;
    }

    @Override
    public String toString() {
        return this.lexeme();
    }

    public String lexeme() {
        return this.nodes().stream().map(n -> n.lexeme()).collect(Collectors.joining("\n"));
    }

    public Map<String, Object> serialize() {
        return Tool.serializeAnnotated(this);
    }

    public Outline infer(Inferences inferences) {
        try {
//            if (this.outline == Unknown) {
            if (!this.inferred()) {
                this.ast().symbolEnv().enter(this.scope());
                this.outline = this.accept(inferences);
                this.ast().symbolEnv().exit();
            }
        } catch (GCPRuntimeException e) {
            ErrorReporter.report(this, e.errCode());
        }
        return outline;
    }

    public boolean inferred() {
//        return this.outline.inferred();
        boolean inferred = this.outline.inferred();;
        if (!inferred) return false;
        for (Node node : this.nodes) {
            inferred = inferred && node.inferred();
            if (!inferred) return false;
        }
        return true;
    }

    public <T> Result<T> interpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }

    protected Outline accept(Inferences inferences) {
//        try {
        return inferences.visit(this);
//        }catch (Exception ex){
//            ErrorReporter.report(GCPErrCode.UNEXPECTED_ERROR,ex.getStackTrace().toString());
//            return Unknown;
//        }
    }

    public int index() {
        return this.parent().nodes().indexOf(this);
    }

    public void markUnknowns() {
        if(this.outline()==Unknown){
            ErrorReporter.report(this,GCPErrCode.INFER_ERROR);
        }
        for (Node node : this.nodes) {
            node.markUnknowns();
        }
    }

    public Node get(int index) {
        return this.nodes().get(index);
    }
}
