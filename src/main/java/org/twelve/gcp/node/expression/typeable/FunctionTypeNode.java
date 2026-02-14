package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionTypeNode extends TypeNode {
    private final List<TypeNode> arguments = new ArrayList<>();
    private final List<ReferenceNode> refs = new ArrayList<>();
    private final TypeNode returns;
    private final Long scope;

    public FunctionTypeNode(AST ast, TypeNode returns, ReferenceNode[] refs, TypeNode[] args) {
        super(ast);
        for (TypeNode arg : args) {
            this.arguments.add(this.addNode(arg));
        }
        if (refs != null) {
            for (ReferenceNode ref : refs) {
                this.refs.add(this.addNode(ref));
            }
        }
        this.returns = this.addNode(returns);
        this.scope = ast.scopeIndexer().incrementAndGet();
    }

    public FunctionTypeNode(AST ast, TypeNode returns, TypeNode... args) {
        this(ast, returns, null, args);
    }

    @Override
    public Long scope() {
        return this.scope;
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        return this.arguments.stream().map(AbstractNode::lexeme).collect(Collectors.joining("->")) +
                "->" + this.returns.lexeme();
    }

    public TypeNode returns() {
        return returns;
    }

    public List<TypeNode> arguments() {
        return arguments;
    }

    public List<ReferenceNode> refs() {
        return refs;
    }
}
