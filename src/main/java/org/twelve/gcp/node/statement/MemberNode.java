package org.twelve.gcp.node.statement;

import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.FieldMergeMode;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.Assignment;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

public class MemberNode extends VariableDeclarator {
    private final FieldMergeMode mergeMode;

    public MemberNode(Identifier name, TypeNode declared, Expression expression, Boolean mutable) {
        this(name, declared, expression, mutable, FieldMergeMode.DEFAULT);
    }

    public MemberNode(Identifier name, TypeNode declared, Expression expression, Boolean mutable, FieldMergeMode mergeMode) {
        super(name.ast(), VariableKind.from(mutable));
        this.mergeMode = mergeMode == null ? FieldMergeMode.DEFAULT : mergeMode;
        this.declare(name, declared, expression);
    }

    public MemberNode(Identifier name, Expression expression, Boolean mutable) {
        this(name, null, expression, mutable);
    }

    public MemberNode(Identifier name, Expression expression, Boolean mutable, FieldMergeMode mergeMode) {
        this(name, null, expression, mutable, mergeMode);
    }

    @Override
    public Assignment declare(Assignable name, TypeNode declared, Expression value) {
        if (!this.assignments().isEmpty()) return null;//member node只能有一个赋值
        return super.declare(name, declared, value);
    }

    @Override
    public Outline outline() {
        return this.identifier().outline();
    }

    @Override
    public String lexeme() {
        return (this.mutable() ? "var " : "") + this.identifier().lexeme() +
                " = " +
                this.expression().lexeme();
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
       return inferencer.visit(this);
//        super.accept(inferences);
//        return this.outline();
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public Identifier identifier() {
        return cast(this.assignments().getFirst().lhs());
    }

    public Expression expression() {
        return this.assignments().getFirst().rhs();
    }

    public Modifier modifier() {
        return this.identifier().modifier();
    }

    public Boolean mutable() {
        return this.kind().mutable();
    }

    public FieldMergeMode mergeMode() {
        return this.mergeMode;
    }
}
