package org.twelve.gcp.builder;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.typeable.FunctionTypeNode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.statement.ExpressionStatement;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.node.statement.Statement;

import java.util.ArrayList;
import java.util.List;

public class FunctionNodeBuilder {
    private final AST ast;
    private List<Argument> arguments = new ArrayList<>();
    private List<Statement> statements =  new ArrayList<>();

    public FunctionNodeBuilder(AST ast) {
        this.ast = ast;
    }
   public FunctionNodeBuilder buildArg(String name, TypeNode declared, Expression expression){
        this.arguments.add(new Argument(new Identifier(ast,new Token<>(name)), declared,expression));
        return this;
   }
    public FunctionNodeBuilder buildArg(String name, TypeNode declared){
        return buildArg(name,declared,null);
    }

    public FunctionNodeBuilder buildArg(String name){
        return buildArg(name,null);
    }
  public FunctionNodeBuilder buildStatement(Statement statement){
        this.statements.add(statement);
        return this;
  }
    public FunctionNodeBuilder buildStatement(Expression e) {
        this.statements.add(new ExpressionStatement(e));
        return this;
    }

 public FunctionNode returns(Expression expression){
     FunctionBody body = new FunctionBody(ast);
     Argument[] args = new Argument[arguments.size()];;
     for(int i=0; i<args.length;i++){
         args[i] = arguments.get(i);
     }
     for (Statement statement : statements) {
         body.addStatement(statement);
     }
     body.addStatement(new ReturnStatement(expression));
     return FunctionNode.from(body,args);
 }


}
