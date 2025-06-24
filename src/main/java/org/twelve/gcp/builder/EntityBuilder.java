package org.twelve.gcp.builder;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inference;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.statement.MemberNode;

import java.util.ArrayList;
import java.util.List;

public class EntityBuilder {
    private final AST ast;
    private List<MemberNode> members = new ArrayList<>();

    public EntityBuilder(AST ast) {
        this.ast = ast;
    }

    public EntityBuilder buildMember(String name, TypeNode declared, Expression expression,Boolean mutable) {
        members.add(new MemberNode(new Identifier(ast,new Token<>(name)),declared,expression,mutable));
        return this;
    }
    public EntityBuilder buildMember(String name, Expression expression,Boolean mutable) {
        return buildMember(name,null,expression,mutable);
    }
    public EntityBuilder buildMember(String name, String value) {
        return buildMember(name,new Identifier(ast,new Token<>(value)),false);
    }
    public EntityBuilder buildMember(String name, Token<?> value) {
        return buildMember(name, LiteralNode.parse(ast,value),false);
    }

    public EntityNode build() {
        return new EntityNode(members);
    }
}
