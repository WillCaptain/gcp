package org.twelve.gcp.inference.operator;

import org.twelve.gcp.common.Associativity;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;

public enum BinaryOperator implements Operator {
    // Arithmetic Operators
    ADD("+", Precedence.ADD_SUB, new AddInference()),
    SUBTRACT("-", Precedence.ADD_SUB, new NumOperaInference()),
    MULTIPLY("*", Precedence.MUL_DIV, new NumOperaInference()),
    DIVIDE("/", Precedence.MUL_DIV, new NumOperaInference()),
    MODULUS("%", Precedence.MUL_DIV, new NumOperaInference()),

    ASSIGN_RETURN(":=", Precedence.COMPARISON, (left, right, node) -> Outline.Error),//forget what to do
    // Comparison Operators
    EQUALS("==", Precedence.COMPARISON, new CompareInference()),
    NOT_EQUALS("!=", Precedence.COMPARISON, new CompareInference()),
    GREATER_THAN(">", Precedence.COMPARISON, new CompareInference()),
    LESS_THAN("<", Precedence.COMPARISON, new CompareInference()),
    GREATER_OR_EQUAL(">=", Precedence.COMPARISON, new CompareInference()),
    LESS_OR_EQUAL("<=", Precedence.COMPARISON, new CompareInference()),

    // Logical Operators
    LOGICAL_AND("&&", Precedence.LOGICAL_AND, new LogicInference()),
    LOGICAL_OR("||", Precedence.LOGICAL_OR, new LogicInference()),

    // Bitwise Operators
    BITWISE_AND("&", Precedence.BITWISE_AND, new BitwiseInference()),
    BITWISE_OR("|", Precedence.BITWISE_OR, new BitwiseInference()),
    BITWISE_XOR("^", Precedence.BITWISE_XOR, new BitwiseInference()),
    BITWISE_SHIFT_LEFT("<<", Precedence.BITWISE_SHIFT, new BitwiseInference()),
    BITWISE_SHIFT_RIGHT(">>", Precedence.BITWISE_SHIFT, new BitwiseInference());

    private final String symbol;
    private final int precedence;
    private final Associativity associativity;
    private final OperatorInference inference;

    BinaryOperator(String symbol, int precedence, Associativity associativity, OperatorInference inference) {
        this.symbol = symbol;
        this.precedence = precedence;
        this.associativity = associativity;
        this.inference = inference;
    }

    BinaryOperator(String symbol, int precedence, OperatorInference inference) {
        this(symbol, precedence, Associativity.LEFT, inference);
    }

    @Override
    public String symbol() {
        return symbol;
    }

    public int precedence() {
        return precedence;
    }

    public Associativity associativity() {
        return associativity;
    }

    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        return this.inference.infer(left, right, node);
    }

    public static BinaryOperator parse(String operator){
        for (BinaryOperator value : BinaryOperator.values()) {
            if(value.symbol().equals(operator)) return value;
        }
        return ADD;
    }
}

