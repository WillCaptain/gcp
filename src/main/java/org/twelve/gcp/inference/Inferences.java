package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.body.WithExpression;
import org.twelve.gcp.node.expression.conditions.*;
import org.twelve.gcp.node.expression.typeable.OptionTypeNode;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.ArrayAccessor;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.IsAs;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.expression.Assignment;
import org.twelve.gcp.node.statement.ExpressionStatement;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.node.unpack.SymbolEntityUnpackNode;
import org.twelve.gcp.node.unpack.TupleUnpackNode;
import org.twelve.gcp.node.unpack.UnpackNode;
import org.twelve.gcp.outline.Outline;

public interface Inferences {
    default Outline visit(Node node){
        for (Node child : node.nodes()) {
            child.infer(this);
        }
        return node.ast().Ignore;
    }
    Outline visit(BinaryExpression binaryExpression);
    Outline visit(UnaryExpression ue);
    Outline visit(Assignment assignment);
    Outline visit(VariableDeclarator assignment);
    Outline visit(Identifier identifier);
    Outline visit(ReferenceNode ref);
    Outline visit(Variable variable);
    Outline visit(Export export);
    Outline visit(Import _import);
    Outline visit(FunctionNode function);
    Outline visit(FunctionCallNode call);
    Outline visit(Argument argument);
    Outline visit(FunctionBody body);
    Outline visit(Block block);
    Outline visit(WithExpression with);
    Outline visit(ReturnStatement body);
    Outline visit(ExpressionStatement body);
    Outline visit(EntityNode entity);
    Outline visit(TupleNode tupleNode);
    Outline visit(MemberAccessor memberAccessor);
    Outline visit(ThisNode me);
    Outline visit(BaseNode baseNode);
    Outline visit(PolyNode poly);
    Outline visit(OptionTypeNode option);
    Outline visit(PolyTypeNode poly);
    Outline visit(Selections selections);
    Outline visit(Arm arm);

    Outline visit(ImportSpecifier importSpecifier);
    Outline visit(ExportSpecifier exportSpecifier);
    Outline visit(IsAs isAs);
    Outline visit(ReferenceCallNode refCallNode);
    Outline visit(IdentifierTypeNode identifierTypeNode);
    Outline visit(FunctionTypeNode functionTypeNode);
    Outline visit(EntityTypeNode entityTypeNode);

    Outline visit(TupleTypeNode tupleTypeNode);

    Outline visit(As asNode);
    Outline visit(ArrayNode arrayNode);
    Outline visit(ArrayTypeNode arrayTypeNode);
    Outline visit(ArrayAccessor arrayAccessor);
    Outline visit(DictNode dictNode);
    Outline visit(DictTypeNode dictTypeNode);

    Outline visit(MatchTest test);

    Outline visit(MatchExpression match);

    Outline visit(UnpackNode unpackNode);

    Outline visit(TupleUnpackNode tupleUnpackNode);

    Outline visit(SymbolIdentifier symbolNode);
    Outline visit(OutlineDefinition outlineDefinition);

    Outline visit(SymbolEntityTypeTypeNode symbolEntityTypeNode);
    Outline visit(SymbolTupleTypeTypeNode symbolTupleTypeNode);
    Outline visit(SymbolEntityUnpackNode unpack);
}
