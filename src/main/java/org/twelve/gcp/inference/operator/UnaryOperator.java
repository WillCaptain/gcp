package org.twelve.gcp.inference.operator;

import org.twelve.gcp.node.expression.UnaryPosition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.twelve.gcp.node.expression.UnaryPosition.POSTFIX;
import static org.twelve.gcp.node.expression.UnaryPosition.PREFIX;

public enum UnaryOperator implements Operator {
    NEGATE("-",PREFIX),           // -a
    INCREMENT("++",PREFIX, POSTFIX),   // ++a, a++
    DECREMENT("--",PREFIX, POSTFIX),   // --a, a--
    BANG("!",PREFIX),       // !a
    BITWISE_NOT("~",PREFIX),       // ~a
    TYPE_OF("typeof",PREFIX),           // typeof a
    TYPE_CAST("(Type)",PREFIX),         // (Type) a
    ADDRESS_OF("&",PREFIX),        // &a (if applicable)
    DEREFERENCE("*",PREFIX);       // *a (if applicable)

    private final Set<UnaryPosition> positions;
    private final String symbol;

    UnaryOperator(String symbol, UnaryPosition... positions) {
        this.symbol = symbol;
        this.positions = new HashSet<>(Arrays.asList(positions));
    }
    public boolean contains(UnaryPosition position){
        return positions.contains(position);
    }

    @Override
    public String symbol() {
        return symbol;
    }

    public static UnaryOperator from(String symbol){
        for (UnaryOperator value : UnaryOperator.values()) {
            if(value.symbol().equals(symbol)){
                return value;
            }
        }
        return null;
    }
}
