package org.twelve.gcp.builder;

import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.ArrayAccessor;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.node.statement.*;

import java.util.Arrays;

public class ASTBuilder {
    private final AST ast;

    public ASTBuilder() {
        ASF asf = new ASF();
        this.ast = asf.newAST();
    }

    public AST ast() {
        return this.ast;
    }

    public void buildReturnStatement(Expression expression) {
        ast.addStatement(new ReturnStatement(expression));
    }

    public void buildVoid(){
        ast.addStatement(new ReturnStatement(ast));
    }

    public void buildExpressionStatement(Expression expression) {
        ast.addStatement(new ExpressionStatement(expression));
    }

    public VariableDeclaratorBuilder buildVariableDeclarator(VariableKind kind,boolean addByDefault) {
        VariableDeclarator var = new VariableDeclarator(ast, kind);
        if(addByDefault) ast.addStatement(var);
        return new VariableDeclaratorBuilder(var);
    }
    public VariableDeclaratorBuilder buildVariableDeclarator(VariableKind kind) {
        return buildVariableDeclarator(kind,true);
    }

    public Identifier buildId(String id) {
        return new Identifier(ast, new Token<>(id));
    }

    public FunctionNodeBuilder buildFunc() {
        return new FunctionNodeBuilder(ast);
    }

    public MemberAccessor buildMemberAccessor(Expression host, String member) {
        return new MemberAccessor(host, buildId(member));
    }
    public MemberAccessor buildMemberAccessor(Expression host, Integer index) {
        return new MemberAccessor(host, index);
    }

    public EntityBuilder buildEntity() {
        return new EntityBuilder(ast);
    }

    public Assignment buildAssignment(Assignable lhs, Expression rhs) {
        return new Assignment(lhs, rhs);
    }

    public Assignment buildAssignment(String lhs, Expression rhs) {
        return buildAssignment(new Identifier(ast, new Token<>(lhs)), rhs);
    }

    public Assignment buildAssignment(String lhs, String rhs) {
        return buildAssignment(lhs, new Identifier(ast, new Token<>(rhs)));
    }

    //    public FunctionCallNode buildCall(String func, String... args) {
//        return buildCall(new Identifier(ast,new Token<>(func)),
//                Arrays.stream(args).map(arg->new Identifier(ast,new Token<>(arg))).toArray(Identifier[]::new));
//    }
    public FunctionCallNode buildCall(Expression func, Expression... args) {
        return new FunctionCallNode(func, args);
    }

    public LiteralNode<?> buildLiteral(String lexeme) {
        return LiteralNode.parse(ast, new Token<>(lexeme));
    }

    public LiteralNode<?> buildLiteral(Integer lexeme) {
        return LiteralNode.parse(ast, new Token<>(lexeme));
    }

    public BinaryExpression buildBinaryOperation(Expression lhs, String operator, Expression rhs) {
        return new BinaryExpression(lhs,rhs, new OperatorNode<>(ast, BinaryOperator.parse(operator)));
    }

    public TupleBuilder buildTuple() {
        return new TupleBuilder(ast);
    }

    public ThisNode buildThis() {
        return new ThisNode(ast, new Token<>("this"));
    }

    public TypeNode buildIdType(String type) {
        return new IdentifierTypeNode(new Identifier(ast,new Token<>(type)));
    }

    public TypeNode buildTupleType(TypeNode... types) {
        return new TupleTypeNode(Arrays.stream(types).toList());

    }

    public TypeNode buildQuestion() {
        return new Question(ast);
    }

    public DictNodeBuilder buildDict() {
        return new DictNodeBuilder(ast);
    }

    public DictTypeNode buildDictType(TypeNode key, TypeNode value) {
        return new DictTypeNode(ast,key,value);
    }

    public ArrayAccessor buildArrayAccessor(Expression host, Expression index) {
        return new ArrayAccessor(ast,host,index);
    }

    public ArrayNodeBuilder buildArray() {
        return new ArrayNodeBuilder(ast);
    }

    public Expression buildRefCall(Identifier host, TypeNode... refs) {
        return new ReferenceCallNode(host,refs);
    }

    public Expression buildBool(String bool) {
        return new BoolNode(ast,new Token<>(bool));
    }
}
