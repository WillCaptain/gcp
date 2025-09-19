package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.ast.UnpackAble;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;

public class Identifier extends Assignable implements UnpackAble {
    private final Token<String> token;

    public Identifier(AST ast, Token<String> token) {
        super(ast, null);
        this.token = token;
    }

    @Override
    public String lexeme() {
        return this.token.lexeme();
    }



    @Override
    public Location loc() {
        return this.token.loc();
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public String name() {
        return this.token.lexeme();
    }

    public Token<String> token() {
        return this.token;
    }


    protected  EnvSymbol lookupSymbol(LocalSymbolEnvironment env, String name){
        return env.lookupSymbol(name);
    }
    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if (this.outline == env.ast().Error) return;
//        EnvSymbol symbol = env.current().lookupSymbol(this.name());
        EnvSymbol symbol = lookupSymbol(env,this.name());
        if (symbol == null) return;
        if (!symbol.outline().inferred()) {
            if (!inferred.canBe(symbol.declared())) {
                GCPErrorReporter.report(this.parent(), GCPErrCode.OUTLINE_MISMATCH);
                return;
            }
            if(!symbol.update(inferred)){
                inferred = inferred.alternative();
               if(!symbol.update(inferred)) {
                   GCPErrorReporter.report(this, GCPErrCode.NOT_BE_ASSIGNEDABLE);
               }
            }
            this.outline = inferred;
            return;

        }
        //handle half declared type like [],[,],entity
        if(symbol.declared().containsUnknown() && inferred.is(this.outline)){
           if(!symbol.update(inferred)){
               inferred = inferred.alternative();
               if(!symbol.update(inferred)) {
                   GCPErrorReporter.report(this, GCPErrCode.NOT_BE_ASSIGNEDABLE);
               }
           }
            this.outline = inferred;
            return;
        }

        //定位与判定是否mutable
        if (symbol.outline() instanceof Poly) {
            Poly poly = cast(symbol.outline());
            //找到poly里匹配的outline
            Outline matched = poly.match(inferred);
            if (matched != null) {
                if (matched == this.ast().Error) {//找到多过一个匹配
                    GCPErrorReporter.report(this, GCPErrCode.AMBIGUOUS_VARIABLE_REFERENCE);
                    return;
                }

                if (!poly.isMutable(matched, symbol.mutable())) {//匹配到了，但是不可赋值
                    GCPErrorReporter.report(this, GCPErrCode.NOT_ASSIGNABLE);
                    return;
                }
            } else {//没有找到匹配，说明赋值类型不一致
                GCPErrorReporter.report(this.parent(), GCPErrCode.OUTLINE_MISMATCH);
                return;
            }
        } else {
            if (!symbol.mutable() && !inferred.is(this.outline)) {//不可赋值
                GCPErrorReporter.report(this, GCPErrCode.NOT_ASSIGNABLE);
                return;
            }
        }
        super.assign(env, inferred);
    }

    public Modifier modifier() {
        return this.lexeme().startsWith("_") ? Modifier.PRIVATE : Modifier.PUBLIC;
    }
}
