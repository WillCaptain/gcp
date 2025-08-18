package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.primitive.*;

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
            return new STRING(this);
        }
        if (value instanceof BigDecimal) {
            return new DECIMAL(this);
        }
        if (value instanceof Double) {
            return new DOUBLE(this);
        }
        if (value instanceof Float) {
            return new FLOAT(this);
        }
        if (value instanceof Long) {
            return new LONG(this);
        }
        if (value instanceof Integer) {
            return new INTEGER(this);
        }
        if (value instanceof Boolean) {
            return new BOOL(this);
        }
        return this.ast().String;
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
