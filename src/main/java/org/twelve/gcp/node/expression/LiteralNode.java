package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.primitive.*;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.math.BigDecimal;

/**
 * literal node
 */
public class LiteralNode<T> extends ValueNode<LiteralNode<?>> {

    public static <D> LiteralNode<D> parse(AST ast, Token<D> token) {
        return new LiteralNode<>(ast, token);
    }

    private final Token<T> token;

    private LiteralNode(AST ast, Token<T> token) {
        super(ast, token.loc());
        this.token = token;
        this.setOutline(this.createOutline(this.value()));
    }
    private ProductADT createOutline(T value){
        if (value instanceof String) {
            return STRING.create(this);
        }
        if (value instanceof BigDecimal) {
            return DECIMAL.create(this);
        }
        if (value instanceof Double) {
            return DOUBLE.create(this);
        }
        if (value instanceof Float) {
            return FLOAT.create(this);
        }
        if (value instanceof Long) {
            return LONG.create(this);
        }
        if (value instanceof Integer) {
            return INTEGER.create(this);
        }
        if (value instanceof Boolean) {
            return new BOOL();
        }
        return Outline.String;
    }

    @Override
    public String lexeme() {
        if (this.outline instanceof STRING) {
            return "\"" + this.token.lexeme() + "\"";
        } else {
            return this.token.lexeme();
        }
    }

    private void setOutline(ProductADT outline){
        this.outline = outline;
    }

//    @Override
//    protected Outline accept(Inferences inferences) {
//        return this.outline;
//    }

    public T value() {
        return this.token.data();
    }

    @Override
    public boolean isSame(LiteralNode obj) {
        return obj.value().equals(this.value());
    }
}
