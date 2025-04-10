package org.twelve.gcp.node.expression.accessor;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.Unknown;

/**
 * a.b
 */
public class MemberAccessor extends Accessor {
    private final Expression entity;
    private final Identifier member;

    public MemberAccessor(AST ast, Expression entity, Identifier member) {
        super(ast);
        this.entity = entity;
        this.member = member;
        this.addNode(this.entity);
        this.addNode(this.member);
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if (this.outline == Outline.Error) return;
        ProductADT owner = cast(this.entity.outline());
        if (!owner.checkMember(member.token(), inferred)) {
            ErrorReporter.report(this, GCPErrCode.OUTLINE_MISMATCH,member.token()+" not found");
        }
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public Expression entity() {
        return this.entity;
    }

    public Identifier member() {
        return this.member;
    }

    @Override
    public String lexeme() {
        return entity.lexeme().split(":")[0].trim() + "." + member.lexeme();
    }

    @Override
    public void markUnknowns() {
        if (this.outline == Unknown) {
            ErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
    }
}
