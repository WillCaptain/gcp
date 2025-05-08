package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.builtin.Namespace;
import org.twelve.gcp.outline.builtin.UNIT;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.adt.ProductADT.Error;
import static org.twelve.gcp.outline.adt.ProductADT.Unknown;

public class Identifier extends Assignable {
    private final Token<String> token;
    private final Boolean mutable;
    private Outline declared;

    // import a as b, b is the reference
    public Identifier(AST ast, Token<String> token) {
        this(ast, token, Unknown, true);
    }

    public Identifier(AST ast, Token<String> token, Outline declared, Boolean mutable) {
        super(ast, null);
        this.token = token;
        this.outline = declared;
        this.declared = declared;
        this.mutable = mutable;
    }

    @Override
    public String lexeme() {

        if ((this.outline() instanceof UNKNOWN) || this.outline instanceof UNIT || this.outline() instanceof Namespace) {
            return this.token.lexeme();
        }

        if (this.isDeclared() || (this.parent().parent() != null && this.parent().parent() instanceof VariableDeclarator && ((Assignment) this.parent()).lhs() == this)) {
            return this.token.lexeme() + ": " + this.outline().toString();
        }
        return this.token.lexeme();
    }

    @Override
    public Location loc() {
        return this.token.loc();
    }

    public Boolean isDeclared() {
        return !(this.declared instanceof UNKNOWN);
    }

    public Boolean isMutable() {
        return this.mutable;
    }

    /**
     * Determines the outline of a variable based on its declaration and assignments:
     * - If the variable is declared, its outline is fixed to the declared type.
     * - If the variable is not declared, its outline is inferred dynamically:
     * - For example:
     * var a = 10; // a: Integer
     * a = "some"; // a: Integer | String
     *
     * @param inferences Contextual inferences used for outline determination.
     * @return The inferred or declared outline of the variable.
     */
    @Override
    public Outline infer(Inferences inferences) {
        if (this.isDeclared()) return this.outline;
//        this.outline = Unknown;
        return super.infer(inferences);
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public String token() {
        return this.token.lexeme();
    }


    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if (this.outline == Error) return;
        EnvSymbol symbol = env.current().lookup(this.token());
        if (symbol == null) return;
        if(!inferred.canBe(symbol.declared())){
            ErrorReporter.report(this.parent(), GCPErrCode.OUTLINE_MISMATCH);
            return;
        }
        //infer is not finished yet
        if (!symbol.outline().inferred()|| symbol.isDeclared()) {
            symbol.update(inferred);
            this.outline = inferred;
            return;

        }
        //定位与判定是否mutable
        if (symbol.outline() instanceof Poly) {
            Poly poly = cast(symbol.outline());
            //找到poly里匹配的outline
            Outline matched = poly.match(inferred);
            if (matched != null) {
                if (matched == Error) {//找到多过一个匹配
                    ErrorReporter.report(this, GCPErrCode.AMBIGUOUS_VARIABLE_REFERENCE);
                    return;
                }

                if (!poly.isMutable(matched, symbol.mutable())) {//匹配到了，但是不可赋值
                    ErrorReporter.report(this, GCPErrCode.NOT_ASSIGNABLE);
                    return;
                }
            } else {//没有找到匹配，说明赋值类型不一致
                ErrorReporter.report(this.parent(), GCPErrCode.OUTLINE_MISMATCH);
                return;
            }
        } else {
            if (!symbol.mutable() && !inferred.is(this.outline)) {//不可赋值
                ErrorReporter.report(this, GCPErrCode.NOT_ASSIGNABLE);
                return;
            }
        }
        super.assign(env, inferred);
    }

    public Modifier modifier() {
        return this.lexeme().startsWith("_") ? Modifier.PRIVATE : Modifier.PUBLIC;
    }

    public void assign(Outline outline) {
        if (this.isDeclared() && !this.outline.tryYouCanBeMe(outline)) {
            ErrorReporter.report(this, GCPErrCode.OUTLINE_MISMATCH);
            return;
        }
        this.outline = outline;
    }

    public Outline declared() {
        return this.declared;
    }
}
