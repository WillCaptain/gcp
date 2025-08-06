package org.twelve.gcp.node.expression.accessor;

import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;

/**
 * a.b
 */
public class MemberAccessor extends Accessor {
    private final Expression productADT;
    private final Identifier member;

    public MemberAccessor(Expression host, Identifier member) {
        super(host.ast());
        this.productADT = host;
        this.member = member;
        this.addNode(this.productADT);
        this.addNode(this.member);
    }
    public MemberAccessor(Expression host, Integer index) {
        this(host,new Identifier(host.ast(),new Token<>(index.toString())));
    }


    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if (this.outline == Outline.Error) return;
        ProductADT owner = cast(this.productADT.outline());
        if (!owner.checkMember(member.name(), inferred)) {
            ErrorReporter.report(this, GCPErrCode.FIELD_NOT_FOUND,
                    member.name() + " not found in " + this.productADT);
        }
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public Expression host() {
        return this.productADT;
    }

    public Identifier member() {
        return this.member;
    }

    @Override
    public String lexeme() {
        return productADT.lexeme().split(":")[0].trim() + "." + member.lexeme();
    }

    @Override
    public void markUnknowns() {
        if (this.outline instanceof UNKNOWN) {
            ErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
    }
}
