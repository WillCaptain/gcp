package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtendTypeNode extends TypeNode {
    private final TypeNode base;
    private final EntityTypeNode extension;
    private final List<ReferenceNode> references = new ArrayList<>();
    private final ReferenceCallTypeNode refCall;

    public ExtendTypeNode(AST ast, List<ReferenceNode> references, TypeNode base, ReferenceCallTypeNode refArgs, EntityTypeNode extension) {
        super(ast);
        this.base = this.addNode(base);
        this.extension = this.addNode(extension);
        for (ReferenceNode reference : references) {
            this.references.add(this.addNode(reference));
        }
        if(refArgs!=null) {
            this.refCall = this.addNode(refArgs);
        }else {
            this.refCall = null;
        }
    }

    public TypeNode base() {
        return this.base;
    }

    public EntityTypeNode extension() {
        return this.extension;
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        if(!this.references.isEmpty()){
            sb.append("<").append(this.references.stream().map(ReferenceNode::lexeme).collect(Collectors.joining(","))).append(">");
        }
        if(this.refCall!=null){
            sb.append(this.refCall.lexeme());
        }else{
            sb.append(this.base.lexeme());
        }
        sb.append(this.extension.lexeme());
        return sb.toString();
    }

    public List<ReferenceNode> references(){
        return this.references;
    }

    public ReferenceCallTypeNode refCall(){
        return this.refCall;
    }
}
