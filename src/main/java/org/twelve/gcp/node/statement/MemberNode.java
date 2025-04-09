package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

public class MemberNode extends VariableDeclarator {

    public MemberNode(OAST ast, Token name, Expression expression, Boolean mutable) {
        super(ast,mutable? VariableKind.VAR:VariableKind.LET);
        this.declare(name,expression);
    }

    @Override
    public Assignment declare(Token varToken, Expression value) {
        if(this.assignments().size()>0) return null;//member node只能有一个赋值
        return super.declare(varToken, value);
    }

    @Override
    public Outline outline() {
        return this.expression().outline();
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder(this.mutable() ? "mute " : "");
        sb.append(this.name().lexeme());
        sb.append(" = ");
        sb.append(this.expression().lexeme());
//        sb.append(",");
        return sb.toString();
    }

    @Override
    protected Outline accept(Inferences inferences) {
        super.accept(inferences);
        return this.outline();
    }

    public Identifier name() {
        return cast(this.assignments().get(0).lhs());
    }

    public Expression expression() {
        return this.assignments().get(0).rhs();
    }

    public Modifier modifier() {
        return this.name().modifier();
    }

    public Boolean mutable() {
        return this.kind()==VariableKind.VAR;
    }

//    @Override
//    public boolean inferred() {
//        return this.expression.inferred();
//    }
}
