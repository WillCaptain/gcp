package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

public class MemberNode extends VariableDeclarator {

    public MemberNode(AST ast, Token<String> name, Expression expression, Boolean mutable) {
        super(ast, VariableKind.from(mutable));
        this.declare(name, expression);
    }

    @Override
    public Assignment declare(Token<String> varToken, Expression value) {
        if (!this.assignments().isEmpty()) return null;//member node只能有一个赋值
        return super.declare(varToken, value);
    }

    @Override
    public Outline outline() {
        return this.expression().outline();
    }

    @Override
    public String lexeme() {
        return (this.mutable() ? "mute " : "") + this.name().lexeme() +
                " = " +
                this.expression().lexeme();
    }

    @Override
    protected Outline accept(Inferences inferences) {
        super.accept(inferences);
        return this.outline();
    }

    public Identifier name() {
        return cast(this.assignments().getFirst().lhs());
    }

    public Expression expression() {
        return this.assignments().getFirst().rhs();
    }

    public Modifier modifier() {
        return this.name().modifier();
    }

    public Boolean mutable() {
        return this.kind().mutable();
    }
}
