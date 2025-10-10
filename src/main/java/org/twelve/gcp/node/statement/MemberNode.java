package org.twelve.gcp.node.statement;

import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

public class MemberNode extends VariableDeclarator {

    public MemberNode(Identifier name, TypeNode declared, Expression expression, Boolean mutable) {
        super(name.ast(), VariableKind.from(mutable));
        this.declare(name, declared, expression);
    }

    public MemberNode(Identifier name, Expression expression, Boolean mutable) {
        this(name, null, expression, mutable);
    }

    @Override
    public Assignment declare(Assignable name, TypeNode declared, Expression value) {
        if (!this.assignments().isEmpty()) return null;//member node只能有一个赋值
        //Identifier name,Expression declared, Expression value
        return super.declare(name, declared, value);
    }

    @Override
    public Outline outline() {
//        return this.expression().outline();
        return this.identifier().outline();
    }

    @Override
    public String lexeme() {
        return (this.mutable() ? "var " : "") + this.identifier().lexeme() +
                " = " +
                this.expression().lexeme();
    }

    @Override
    public Outline accept(Inferences inferences) {
        super.accept(inferences);
        return this.outline();
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
}
