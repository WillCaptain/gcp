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
public class LiteralNode<T, O extends Primitive> extends ValueNode<LiteralNode<?,?>> {

    public static LiteralNode parse(AST ast, Token token) {
        Object value = token.data();// LiteralParser.parse(token.lexeme());
        if (value instanceof String) {
            LiteralNode node = new LiteralNode<>(ast, token,  (String) value);
            return node.setOutline(new STRING(node));
        }
        if (value instanceof BigDecimal) {
            LiteralNode node = new LiteralNode<>(ast, token, (BigDecimal) value);
            return node.setOutline(new DECIMAL(node));
        }
        if (value instanceof Double) {
            LiteralNode node = new LiteralNode<>(ast, token, (Double) value);
            return node.setOutline(new DOUBLE(node));
        }
        if (value instanceof Float) {
            LiteralNode node =  new LiteralNode<>(ast, token, (Float) value);
            return node.setOutline(new FLOAT(node));
        }
        if (value instanceof Long) {
            LiteralNode node =  new LiteralNode<>(ast, token, (Long) value);
            return node.setOutline(new LONG(node));
        }
        if (value instanceof Integer) {
            LiteralNode node =  new LiteralNode<>(ast, token, (Integer) value);
            return node.setOutline(new INTEGER(node));
        }
        return null;
    }

    private final Token token;
    private final T value;

    private LiteralNode(AST ast, Token token, T value) {
        super(ast, token.loc());
        this.token = token;
        this.value = value;
//        this.outline = outline;
    }

    @Override
    public String lexeme() {
        if (this.outline instanceof STRING) {
            return "\"" + this.token.lexeme() + "\"";
        } else {
            return this.token.lexeme();
        }
    }

    public LiteralNode setOutline(ProductADT outline){
        this.outline = outline;
        return this;
    }

    public T value() {
        return this.value;
    }

    @Override
    public boolean isSame(LiteralNode obj) {
        return obj.value.equals(this.value);
    }
}
