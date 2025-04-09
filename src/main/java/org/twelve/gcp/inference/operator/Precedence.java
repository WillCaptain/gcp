package org.twelve.gcp.inference.operator;

public class Precedence {
    public static final int MUL_DIV = 5;
    public static final int ADD_SUB = 4;
    public static final int COMPARISON = 3;
    public static final int BITWISE_SHIFT = 2;
    public static final int BITWISE_AND = 1;
    public static final int BITWISE_OR = 1;
    public static final int BITWISE_XOR = 1;
    public static final int LOGICAL_AND = 0;
    public static final int LOGICAL_OR = 0;
}
