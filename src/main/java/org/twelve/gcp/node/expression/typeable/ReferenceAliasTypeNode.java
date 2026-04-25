package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReferenceAliasTypeNode extends TypeNode {
    private final List<ReferenceNode> refs = new ArrayList<>();
    private final TypeNode body;

    public ReferenceAliasTypeNode(AST ast, List<ReferenceNode> refs, TypeNode body) {
        super(ast);
        for (ReferenceNode ref : refs) {
            this.refs.add(this.addNode(ref));
        }
        this.body = this.addNode(body);
    }

    public List<ReferenceNode> refs() {
        return this.refs;
    }

    public TypeNode body() {
        return this.body;
    }

    @Override
    public String lexeme() {
        return "<" + refs.stream().map(ReferenceNode::lexeme).collect(Collectors.joining(","))
                + ">(" + body.lexeme() + ")";
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
}
