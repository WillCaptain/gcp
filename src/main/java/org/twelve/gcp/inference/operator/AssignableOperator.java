package org.twelve.gcp.inference.operator;

import org.twelve.gcp.node.expression.UnaryPosition;

public enum AssignableOperator implements Operator{
    ADD_EQUALS("+=",true),
    SUB_EQUALS("-=",true),
    MULTIPLY_EQUALS("*=",true),
    DIVIDE_EQUALS("/=",true),
    EQUALS("=",false);
    private final String symbol;
    private final Boolean numberOnly;
    AssignableOperator(String symbol,Boolean numberOnly) {
        this.symbol = symbol;
        this.numberOnly = numberOnly;
    }
    public boolean numberOnly(UnaryPosition position){
        return numberOnly;
    }

    @Override
    public String symbol() {
        return symbol;
    }

    public static AssignableOperator from(String symbol){
        for (AssignableOperator value : AssignableOperator.values()) {
            if(value.symbol().equals(symbol)){
                return value;
            }
        }
        return null;
    }
}
