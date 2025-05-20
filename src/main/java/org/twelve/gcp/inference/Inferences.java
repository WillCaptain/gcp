package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.LiteralUnionNode;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.Selections;
import org.twelve.gcp.node.expression.IsAs;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.node.statement.ExpressionStatement;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.node.expression.typeable.IdentifierTypeNode;
import org.twelve.gcp.outline.Outline;

public interface Inferences {
    default Outline visit(Node node){
        for (Node child : node.nodes()) {
            child.infer(this);
        }
        return Outline.Ignore;
    }
    Outline visit(BinaryExpression binaryExpression);

    Outline visit(UnaryExpression ue);
    Outline visit(TernaryExpression te);
    Outline visit(Assignment assignment);
    Outline visit(VariableDeclarator assignment);
    Outline visit(Identifier identifier);
    Outline visit(IdentifierTypeNode identifierTypeNode);
    Outline visit(ReferenceNode ref);
    Outline visit(Variable variable);
    Outline visit(Export export);
    Outline visit(Import _import);
    Outline visit(FunctionNode function);
    Outline visit(FunctionCallNode call);
    Outline visit(Argument argument);
    Outline visit(FunctionBody body);
    Outline visit(Block block);
    Outline visit(ReturnStatement body);
    Outline visit(ExpressionStatement body);
    Outline visit(EntityNode entity);
    Outline visit(MemberAccessor memberAccessor);
    Outline visit(This me);
    Outline visit(Base base);
    Outline visit(PolyNode poly);
    Outline visit(LiteralUnionNode union);
    Outline visit(Selections selections);
    Outline visit(Arm arm);
    Outline visit(ImportSpecifier importSpecifier);
    Outline visit(ExportSpecifier exportSpecifier);
    Outline visit(IsAs isAs);
    Outline visit(ReferenceCallNode refCallNode);
}
