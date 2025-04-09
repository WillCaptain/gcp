package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Generic;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.Unit;
import static org.twelve.gcp.outline.Outline.Unknown;

public class Argument extends ONode {
    //    private static Argument unit = null;
    private int index = 0;

    public static Argument unit(OAST ast) {
//        if (unit == null) {
//            unit = new Argument(ast, Token.unit(), Unit);
//        }
//        return unit;
        return new Argument(ast, Token.unit(), Unit);
    }

    private final Identifier identifier;
    protected final Expression defaultValue;

    public Argument(OAST ast, Token token) {
        this(ast, token, (Expression) null);
    }

    public Argument(OAST ast, Token token, Expression reference) {
        this(ast, token, null, reference);
    }

    public Argument(OAST ast, Token token, Outline outline, Expression defaultValue) {
        super(ast);
        this.identifier = this.addNode(outline == null ? new Identifier(ast, token) :
                new Identifier(ast, token, outline, false));
        this.defaultValue = defaultValue;
    }

    public Argument(OAST ast, Token token, Outline outline) {
        this(ast, token, outline, null);
    }


    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public Identifier identifier() {
        return identifier;
    }

    public Expression defaultValue() {
        return this.defaultValue;
    }

    @Override
    public String lexeme() {
        return this.identifier.lexeme();
    }

    @Override
    public Generic outline() {
        return cast(super.outline());
    }

    public Argument setIndex(int index) {
        this.index = index;
        return this;
    }

//    @Override
//    public boolean inferred() {
//        return this.outline != Unknown;
//    }

    @Override
    public void markUnknowns() {
        if (this.outline == Unknown) {
            ErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
    }

    @Override
    public int index() {
        return this.index;
    }
}
