package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityTypeNode extends TypeNode {
    protected final List<Variable> members = new ArrayList<>();
    private final List<ReferenceNode> refs = new ArrayList<>();
    private final long scope;
    /** field name → default-value literal node (for `alias: "alice"` style declarations) */
    private final Map<String, Node> defaults = new java.util.LinkedHashMap<>();

    public EntityTypeNode(List<Variable> members) {
        this(new ArrayList<>(), members);
    }

    public EntityTypeNode(AST ast) {
        super(ast);
        this.scope = ast.scopeIndexer().incrementAndGet();
    }

    public EntityTypeNode(List<ReferenceNode> refs, List<Variable> members) {
        this(members.getFirst().ast());
        for (Variable member : members) {
            this.members.add(this.addNode(member));
        }
        for (ReferenceNode ref : refs) {
            this.refs.add(this.addNode(ref));
        }
    }

    public List<Variable> members() {
        return this.members;
    }

    public List<ReferenceNode> refs() {
        return this.refs;
    }

    /** Returns the default-value node for the given field, or {@code null} if none. */
    public Node getDefault(String fieldName) {
        return this.defaults.get(fieldName);
    }

    /** Registers a default-value literal node for the given field. */
    public void addDefault(String fieldName, Node defaultValueNode) {
        this.defaults.put(fieldName, defaultValueNode);
        // Set parent so that scope-chain walking (e.g. ThisInference) can reach this
        // node's IN_PRODUCT_ADT scope from within the default-value expression's body.
        defaultValueNode.setParent(this);
    }

    public Map<String, Node> defaults() {
        return this.defaults;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        if (members.isEmpty()) return sb.append("{}").toString();
        if (!this.refs.isEmpty()) {
            sb.append("<");
            sb.append(this.refs.stream().map(ReferenceNode::lexeme).collect(Collectors.joining(",")));
            sb.append(">");
        }
        sb.append("{");
        sb.append(members.stream().map(m -> (m.mutable() ? "var " : "") + m.lexeme()).collect(Collectors.joining(",")));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    @Override
    public Long scope() {
        return this.scope;
    }
}
