package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.typeable.TypeAble;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.projectable.Generic;

import static org.twelve.gcp.common.Tool.cast;

public class Argument extends Identifier {
    private int index = 0;

    public static Argument unit(AST ast) {
        return new Argument(ast, Token.unit(), null);
    }

    protected final Expression defaultValue;
    private final TypeAble declared;

    public Argument(AST ast, Token<String> token) {
        this(ast, token, null);
    }

    public Argument(AST ast, Token<String> token, TypeAble declared, Expression defaultValue) {
        super(ast, token);
        this.declared = declared;
        this.defaultValue = defaultValue;
    }

    public Argument(AST ast, Token<String> token, TypeAble declared) {
        this(ast, token, declared, null);
    }


    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public Expression defaultValue() {
        return this.defaultValue;
    }

    @Override
    public String lexeme() {
        String ext = "";
        if(this.declared!=null){
            ext = ": ";
            if(this.declared instanceof Identifier){
                ext += this.declared.lexeme();
            }else{
                ext += this.declared.outline();
            }
        }
        return this.name() + ext;
    }

    @Override
    public Generic outline() {
        return cast(super.outline());
    }

    public Argument setIndex(int index) {
        this.index = index;
        return this;
    }

    @Override
    public boolean inferred() {
        return this.outline.inferred();
    }

    @Override
    public void markUnknowns() {
        if (this.outline instanceof UNKNOWN) {
            ErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
    }

    @Override
    public int index() {
        return this.index;
    }

    public TypeAble declared() {
        return this.declared;
    }
}
