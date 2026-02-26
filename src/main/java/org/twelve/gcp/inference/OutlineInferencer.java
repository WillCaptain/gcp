package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.body.WithExpression;
import org.twelve.gcp.node.expression.conditions.*;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
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
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.node.unpack.*;
import org.twelve.gcp.outline.Outline;


public class OutlineInferencer implements Inferencer {
    private final Boolean isLazy;

    public OutlineInferencer(Boolean isLazy){
        this.isLazy = isLazy;
    }
    public OutlineInferencer(){
        this(false);
    }

    @Override
    public Boolean isLazy(){
        return this.isLazy;
    }

    @Override
    public Outline visit(BinaryExpression be) {
        return new BinaryExprInference().infer(be, this);
    }

    @Override
    public Outline visit(UnaryExpression ue) {
        return new UnaryExprInference().infer(ue, this);
    }

    @Override
    public Outline visit(Selections selections) {
        return new SelectionsInference().infer(selections, this);
    }

    @Override
    public Outline visit(Assignment assignment) {
        return new AssignmentInference().infer(assignment, this);
    }

    @Override
    public Outline visit(VariableDeclarator variableDeclarator) {
        return new VariableDeclaratorInference().infer(variableDeclarator, this);
    }

    @Override
    public Outline visit(MemberNode memberNode) {
        return new MemberNodeInference().infer(memberNode, this);
    }

    @Override
    public Outline visit(Identifier identifier) {
        return new IdentifierInference().infer(identifier, this);
    }

    @Override
    public Outline visit(IdentifierTypeNode identifierTypeNode) {
        return new IdentifierTypeNodeInference().infer(identifierTypeNode, this);
    }

    @Override
    public Outline visit(FunctionTypeNode functionTypeNode) {
        return new FunctionTypeNodeInference().infer(functionTypeNode, this);
    }

    @Override
    public Outline visit(EntityTypeNode entityTypeNode) {
        return new EntityTypeNodeInference().infer(entityTypeNode, this);
    }

    @Override
    public Outline visit(TupleTypeNode tupleTypeNode) {
        return new TupleTypeNodeInference().infer(tupleTypeNode, this);
    }

    @Override
    public Outline visit(As asNode) {
        return new AsInference().infer(asNode, this);
    }

    @Override
    public Outline visit(ArrayNode arrayNode) {
        return new ArrayNodeInference().infer(arrayNode, this);
    }

    @Override
    public Outline visit(ArrayTypeNode arrayTypeNode) {
        return new ArrayTypeNodeInference().infer(arrayTypeNode, this);
    }

    @Override
    public Outline visit(ArrayAccessor arrayAccessor) {
        return new ArrayAccessorInference().infer(arrayAccessor, this);
    }

    @Override
    public Outline visit(DictNode dictNode) {
        return new DictNodeInference().infer(dictNode, this);
    }

    @Override
    public Outline visit(DictTypeNode dictTypeNode) {
        return new DictTypeNodeInference().infer(dictTypeNode, this);
    }

    @Override
    public Outline visit(ReferenceCallTypeNode referenceCallTypeNode) {
        return new ReferenceCallTypeNodeInference().infer(referenceCallTypeNode,this);
    }

    @Override
    public Outline visit(ReferenceNode ref) {
        return new ReferenceNodeInference().infer(ref, this);
    }

    @Override
    public Outline visit(Variable variable) {
        return new VariableInference().infer(variable, this);
    }

    @Override
    public Outline visit(Export export) {
        return new ExportInference().infer(export, this);
    }

    @Override
    public Outline visit(Import _import) {
        return new ImportInference().infer(_import, this);
    }

    @Override
    public Outline visit(Argument argument) {
        return new ArgumentInference().infer(argument, this);
    }

    @Override
    public Outline visit(FunctionBody body) {
        return new FunctionBodyInference().infer(body, this);
    }

    @Override
    public Outline visit(Block block) {
        return new BlockInference().infer(block, this);
    }

    @Override
    public Outline visit(WithExpression with) {
        return new WithExpressionInference().infer(with, this);
    }

    @Override
    public Outline visit(ReturnStatement returns) {
        return new ReturnInference().infer(returns, this);
    }

    @Override
    public Outline visit(ExpressionStatement body) {
        return new ExprStatementInference().infer(body, this);
    }

    @Override
    public Outline visit(EntityNode entity) {
        return new EntityInference().infer(entity, this);
    }

    @Override
    public Outline visit(TupleNode tuple) {
        return new TupleInference().infer(tuple, this);
    }

    @Override
    public Outline visit(MemberAccessor memberAccessor) {
        return new MemberAccessorInference().infer(memberAccessor, this);
    }

    @Override
    public Outline visit(ThisNode me) {
        return new ThisInference().infer(me, this);
    }

    @Override
    public Outline visit(BaseNode baseNode) {
        return new BaseInference().infer(baseNode, this);
    }

    @Override
    public Outline visit(PolyNode poly) {
        return new PolyInference().infer(poly, this);
    }

    @Override
    public Outline visit(OptionTypeNode option) {
        return new OptionTypeInference().infer(option, this);
    }

    @Override
    public Outline visit(PolyTypeNode poly) {
        return new PolyTypeInference().infer(poly, this);
    }

    @Override
    public Outline visit(Arm arm) {
        return new ArmInference().infer(arm, this);
    }

    @Override
    public Outline visit(ImportSpecifier importSpecifier) {
        return new ImportSpecifierInference().infer(importSpecifier, this);
    }

    @Override
    public Outline visit(ExportSpecifier exportSpecifier) {
        return new ExportSpecifierInference().infer(exportSpecifier, this);
    }

    @Override
    public Outline visit(IsAs isAs) {
        return new IsAsInference().infer(isAs, this);
    }

    @Override
    public Outline visit(ReferenceCallNode refCallNode) {
        return new ReferenceCallInference().infer(refCallNode, this);
    }


    @Override
    public Outline visit(FunctionNode function) {
        return new FunctionInference().infer(function, this);
    }

    @Override
    public Outline visit(FunctionCallNode call) {
        return new FunctionCallInference().infer(call, this);
    }

    @Override
    public Outline visit(MatchTest test) {
        return new MatchTestInference().infer(test, this);
    }

    @Override
    public Outline visit(MatchExpression match) {
        return new MatchExpressionInference().infer(match, this);
    }

    @Override
    public Outline visit(UnpackNode unpackNode) {
        return new UnpackNodeInference().infer(unpackNode, this);
    }

    @Override
    public Outline visit(TupleUnpackNode tupleUnpackNode) {
        return new TupleUnpackNodeInference().infer(tupleUnpackNode,this);
    }

    @Override
    public Outline visit(EntityUnpackNode entityUnpackNode) {
        return new EntityUnpackNodeInference().infer(entityUnpackNode,this);
    }

    @Override
    public Outline visit(SymbolTupleUnpackNode unpack) {
        return new SymbolTupleUnPackNodeInference().infer(unpack,this);
    }


    @Override
    public Outline visit(SymbolEntityUnpackNode unpack) {
        return new SymbolEntityUnpackNodeInference().infer(unpack,this);

    }

    @Override
    public Outline visit(SymbolIdentifier symbolNode){
        return new SymbolIdentifierInference().infer(symbolNode,this);
    }

    @Override
    public Outline visit(OutlineDefinition outlineDefinition) {
        return new OutlineDefinitionInference().infer(outlineDefinition,this);
    }

    @Override
    public Outline visit(SymbolEntityTypeTypeNode symbolEntityTypeNode) {
        return new SymbolEntityTypeNodeInference().infer(symbolEntityTypeNode,this);
    }

    @Override
    public Outline visit(SymbolTupleTypeTypeNode symbolTupleTypeNode) {
        return new SymbolTupleTypeNodeInference().infer(symbolTupleTypeNode,this);
    }

    @Override
    public Outline visit(ThisTypeNode thisTypeNode) {
        return new ThisTypeNodeInference().infer(thisTypeNode,this);
    }

    @Override
    public Outline visit(ExtendTypeNode extendTypeNode) {
        return new ExtendTypeNodeInference().infer(extendTypeNode,this);
    }
}
