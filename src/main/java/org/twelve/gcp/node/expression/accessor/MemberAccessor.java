package org.twelve.gcp.node.expression.accessor;

import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

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
        if (this.outline == this.ast().Error) return;
        ProductADT owner = cast(((ProductADT) this.productADT.outline()).eventual());
        if (!owner.checkMember(member.name(), inferred)) {
            GCPErrorReporter.report(this, GCPErrCode.FIELD_NOT_FOUND,
                    member.name() + " not found in " + this.productADT);
        }
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public Expression host() {
        return this.productADT;
    }

    public Identifier member() {
        return this.member;
    }

    @Override
    public String lexeme() {
//        return productADT.lexeme().split(":")[0].trim() + "." + member.lexeme();
        return productADT.lexeme().trim() + "." + member.lexeme();
    }

    @Override
    public void clearError() {
        super.clearError();
        // Also clear stale errors on the member identifier (e.g. FIELD_NOT_FOUND
        // reported by MemberAccessorInference on node.member()), because the member
        // identifier is never independently re-inferred and its clearError() is never
        // called through the normal inference chain.
        this.member.clearError();
    }

    @Override
    public void markUnknowns() {
        if (this.outline instanceof UNKNOWN) {
            GCPErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
    }
}
