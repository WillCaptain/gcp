package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.AsyncNode;
import org.twelve.gcp.node.expression.AwaitNode;
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

    // All Inference objects are stateless — one singleton per type eliminates per-node allocation.
    private static final BinaryExprInference            BINARY_EXPR              = new BinaryExprInference();
    private static final UnaryExprInference             UNARY_EXPR               = new UnaryExprInference();
    private static final SelectionsInference            SELECTIONS               = new SelectionsInference();
    private static final AssignmentInference            ASSIGNMENT               = new AssignmentInference();
    private static final VariableDeclaratorInference    VARIABLE_DECLARATOR      = new VariableDeclaratorInference();
    private static final MemberNodeInference            MEMBER_NODE              = new MemberNodeInference();
    private static final IdentifierInference            IDENTIFIER               = new IdentifierInference();
    private static final IdentifierTypeNodeInference    IDENTIFIER_TYPE_NODE     = new IdentifierTypeNodeInference();
    private static final FunctionTypeNodeInference      FUNCTION_TYPE_NODE       = new FunctionTypeNodeInference();
    private static final EntityTypeNodeInference        ENTITY_TYPE_NODE         = new EntityTypeNodeInference();
    private static final TupleTypeNodeInference         TUPLE_TYPE_NODE          = new TupleTypeNodeInference();
    private static final AsInference                    AS                       = new AsInference();
    private static final ArrayNodeInference             ARRAY_NODE               = new ArrayNodeInference();
    private static final ArrayTypeNodeInference         ARRAY_TYPE_NODE          = new ArrayTypeNodeInference();
    private static final ArrayAccessorInference         ARRAY_ACCESSOR           = new ArrayAccessorInference();
    private static final DictNodeInference              DICT_NODE                = new DictNodeInference();
    private static final DictTypeNodeInference          DICT_TYPE_NODE           = new DictTypeNodeInference();
    private static final ReferenceCallTypeNodeInference REF_CALL_TYPE_NODE       = new ReferenceCallTypeNodeInference();
    private static final ReferenceNodeInference         REFERENCE_NODE           = new ReferenceNodeInference();
    private static final VariableInference              VARIABLE                 = new VariableInference();
    private static final ExportInference                EXPORT                   = new ExportInference();
    private static final ImportInference                IMPORT                   = new ImportInference();
    private static final ArgumentInference              ARGUMENT                 = new ArgumentInference();
    private static final FunctionBodyInference          FUNCTION_BODY            = new FunctionBodyInference();
    private static final BlockInference                 BLOCK                    = new BlockInference();
    private static final WithExpressionInference        WITH_EXPRESSION          = new WithExpressionInference();
    private static final ReturnInference                RETURN                   = new ReturnInference();
    private static final ExprStatementInference         EXPR_STATEMENT           = new ExprStatementInference();
    private static final EntityInference                ENTITY                   = new EntityInference();
    private static final TupleInference                 TUPLE                    = new TupleInference();
    private static final MemberAccessorInference        MEMBER_ACCESSOR          = new MemberAccessorInference();
    private static final ThisInference                  THIS                     = new ThisInference();
    private static final BaseInference                  BASE                     = new BaseInference();
    private static final PolyInference                  POLY                     = new PolyInference();
    private static final OptionTypeInference            OPTION_TYPE              = new OptionTypeInference();
    private static final PolyTypeInference              POLY_TYPE                = new PolyTypeInference();
    private static final ArmInference                   ARM                      = new ArmInference();
    private static final ImportSpecifierInference       IMPORT_SPECIFIER         = new ImportSpecifierInference();
    private static final ExportSpecifierInference       EXPORT_SPECIFIER         = new ExportSpecifierInference();
    private static final IsAsInference                  IS_AS                    = new IsAsInference();
    private static final ReferenceCallInference         REFERENCE_CALL           = new ReferenceCallInference();
    private static final FunctionInference              FUNCTION                 = new FunctionInference();
    private static final FunctionCallInference          FUNCTION_CALL            = new FunctionCallInference();
    private static final MatchTestInference             MATCH_TEST               = new MatchTestInference();
    private static final MatchExpressionInference       MATCH_EXPRESSION         = new MatchExpressionInference();
    private static final UnpackNodeInference            UNPACK_NODE              = new UnpackNodeInference();
    private static final TupleUnpackNodeInference       TUPLE_UNPACK_NODE        = new TupleUnpackNodeInference();
    private static final EntityUnpackNodeInference      ENTITY_UNPACK_NODE       = new EntityUnpackNodeInference();
    private static final SymbolTupleUnPackNodeInference SYMBOL_TUPLE_UNPACK_NODE = new SymbolTupleUnPackNodeInference();
    private static final SymbolEntityUnpackNodeInference SYMBOL_ENTITY_UNPACK   = new SymbolEntityUnpackNodeInference();
    private static final SymbolIdentifierInference      SYMBOL_IDENTIFIER        = new SymbolIdentifierInference();
    private static final OutlineDefinitionInference     OUTLINE_DEFINITION       = new OutlineDefinitionInference();
    private static final SymbolEntityTypeNodeInference  SYMBOL_ENTITY_TYPE_NODE  = new SymbolEntityTypeNodeInference();
    private static final SymbolTupleTypeNodeInference   SYMBOL_TUPLE_TYPE_NODE   = new SymbolTupleTypeNodeInference();
    private static final ThisTypeNodeInference          THIS_TYPE_NODE           = new ThisTypeNodeInference();
    private static final ExtendTypeNodeInference        EXTEND_TYPE_NODE         = new ExtendTypeNodeInference();
    private static final AsyncInference                 ASYNC                    = new AsyncInference();
    private static final AwaitInference                 AWAIT                    = new AwaitInference();

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
        return BINARY_EXPR.infer(be, this);
    }

    @Override
    public Outline visit(UnaryExpression ue) {
        return UNARY_EXPR.infer(ue, this);
    }

    @Override
    public Outline visit(Selections selections) {
        return SELECTIONS.infer(selections, this);
    }

    @Override
    public Outline visit(Assignment assignment) {
        return ASSIGNMENT.infer(assignment, this);
    }

    @Override
    public Outline visit(VariableDeclarator variableDeclarator) {
        return VARIABLE_DECLARATOR.infer(variableDeclarator, this);
    }

    @Override
    public Outline visit(MemberNode memberNode) {
        return MEMBER_NODE.infer(memberNode, this);
    }

    @Override
    public Outline visit(Identifier identifier) {
        return IDENTIFIER.infer(identifier, this);
    }

    @Override
    public Outline visit(IdentifierTypeNode identifierTypeNode) {
        return IDENTIFIER_TYPE_NODE.infer(identifierTypeNode, this);
    }

    @Override
    public Outline visit(FunctionTypeNode functionTypeNode) {
        return FUNCTION_TYPE_NODE.infer(functionTypeNode, this);
    }

    @Override
    public Outline visit(EntityTypeNode entityTypeNode) {
        return ENTITY_TYPE_NODE.infer(entityTypeNode, this);
    }

    @Override
    public Outline visit(TupleTypeNode tupleTypeNode) {
        return TUPLE_TYPE_NODE.infer(tupleTypeNode, this);
    }

    @Override
    public Outline visit(As asNode) {
        return AS.infer(asNode, this);
    }

    @Override
    public Outline visit(ArrayNode arrayNode) {
        return ARRAY_NODE.infer(arrayNode, this);
    }

    @Override
    public Outline visit(ArrayTypeNode arrayTypeNode) {
        return ARRAY_TYPE_NODE.infer(arrayTypeNode, this);
    }

    @Override
    public Outline visit(ArrayAccessor arrayAccessor) {
        return ARRAY_ACCESSOR.infer(arrayAccessor, this);
    }

    @Override
    public Outline visit(DictNode dictNode) {
        return DICT_NODE.infer(dictNode, this);
    }

    @Override
    public Outline visit(DictTypeNode dictTypeNode) {
        return DICT_TYPE_NODE.infer(dictTypeNode, this);
    }

    @Override
    public Outline visit(ReferenceCallTypeNode referenceCallTypeNode) {
        return REF_CALL_TYPE_NODE.infer(referenceCallTypeNode, this);
    }

    @Override
    public Outline visit(ReferenceNode ref) {
        return REFERENCE_NODE.infer(ref, this);
    }

    @Override
    public Outline visit(Variable variable) {
        return VARIABLE.infer(variable, this);
    }

    @Override
    public Outline visit(Export export) {
        return EXPORT.infer(export, this);
    }

    @Override
    public Outline visit(Import _import) {
        return IMPORT.infer(_import, this);
    }

    @Override
    public Outline visit(Argument argument) {
        return ARGUMENT.infer(argument, this);
    }

    @Override
    public Outline visit(FunctionBody body) {
        return FUNCTION_BODY.infer(body, this);
    }

    @Override
    public Outline visit(Block block) {
        return BLOCK.infer(block, this);
    }

    @Override
    public Outline visit(WithExpression with) {
        return WITH_EXPRESSION.infer(with, this);
    }

    @Override
    public Outline visit(ReturnStatement returns) {
        return RETURN.infer(returns, this);
    }

    @Override
    public Outline visit(ExpressionStatement body) {
        return EXPR_STATEMENT.infer(body, this);
    }

    @Override
    public Outline visit(EntityNode entity) {
        return ENTITY.infer(entity, this);
    }

    @Override
    public Outline visit(TupleNode tuple) {
        return TUPLE.infer(tuple, this);
    }

    @Override
    public Outline visit(MemberAccessor memberAccessor) {
        return MEMBER_ACCESSOR.infer(memberAccessor, this);
    }

    @Override
    public Outline visit(ThisNode me) {
        return THIS.infer(me, this);
    }

    @Override
    public Outline visit(BaseNode baseNode) {
        return BASE.infer(baseNode, this);
    }

    @Override
    public Outline visit(PolyNode poly) {
        return POLY.infer(poly, this);
    }

    @Override
    public Outline visit(OptionTypeNode option) {
        return OPTION_TYPE.infer(option, this);
    }

    @Override
    public Outline visit(PolyTypeNode poly) {
        return POLY_TYPE.infer(poly, this);
    }

    @Override
    public Outline visit(Arm arm) {
        return ARM.infer(arm, this);
    }

    @Override
    public Outline visit(ImportSpecifier importSpecifier) {
        return IMPORT_SPECIFIER.infer(importSpecifier, this);
    }

    @Override
    public Outline visit(ExportSpecifier exportSpecifier) {
        return EXPORT_SPECIFIER.infer(exportSpecifier, this);
    }

    @Override
    public Outline visit(IsAs isAs) {
        return IS_AS.infer(isAs, this);
    }

    @Override
    public Outline visit(ReferenceCallNode refCallNode) {
        return REFERENCE_CALL.infer(refCallNode, this);
    }

    @Override
    public Outline visit(FunctionNode function) {
        return FUNCTION.infer(function, this);
    }

    @Override
    public Outline visit(FunctionCallNode call) {
        return FUNCTION_CALL.infer(call, this);
    }

    @Override
    public Outline visit(MatchTest test) {
        return MATCH_TEST.infer(test, this);
    }

    @Override
    public Outline visit(MatchExpression match) {
        return MATCH_EXPRESSION.infer(match, this);
    }

    @Override
    public Outline visit(UnpackNode unpackNode) {
        return UNPACK_NODE.infer(unpackNode, this);
    }

    @Override
    public Outline visit(TupleUnpackNode tupleUnpackNode) {
        return TUPLE_UNPACK_NODE.infer(tupleUnpackNode, this);
    }

    @Override
    public Outline visit(EntityUnpackNode entityUnpackNode) {
        return ENTITY_UNPACK_NODE.infer(entityUnpackNode, this);
    }

    @Override
    public Outline visit(SymbolTupleUnpackNode unpack) {
        return SYMBOL_TUPLE_UNPACK_NODE.infer(unpack, this);
    }

    @Override
    public Outline visit(SymbolEntityUnpackNode unpack) {
        return SYMBOL_ENTITY_UNPACK.infer(unpack, this);
    }

    @Override
    public Outline visit(SymbolIdentifier symbolNode) {
        return SYMBOL_IDENTIFIER.infer(symbolNode, this);
    }

    @Override
    public Outline visit(OutlineDefinition outlineDefinition) {
        return OUTLINE_DEFINITION.infer(outlineDefinition, this);
    }

    @Override
    public Outline visit(SymbolEntityTypeTypeNode symbolEntityTypeNode) {
        return SYMBOL_ENTITY_TYPE_NODE.infer(symbolEntityTypeNode, this);
    }

    @Override
    public Outline visit(SymbolTupleTypeTypeNode symbolTupleTypeNode) {
        return SYMBOL_TUPLE_TYPE_NODE.infer(symbolTupleTypeNode, this);
    }

    @Override
    public Outline visit(ThisTypeNode thisTypeNode) {
        return THIS_TYPE_NODE.infer(thisTypeNode, this);
    }

    @Override
    public Outline visit(ExtendTypeNode extendTypeNode) {
        return EXTEND_TYPE_NODE.infer(extendTypeNode, this);
    }

    @Override
    public Outline visit(AsyncNode asyncNode) {
        return ASYNC.infer(asyncNode, this);
    }

    @Override
    public Outline visit(AwaitNode awaitNode) {
        return AWAIT.infer(awaitNode, this);
    }
}
