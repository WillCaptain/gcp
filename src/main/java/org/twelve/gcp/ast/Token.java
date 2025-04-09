package org.twelve.gcp.ast;

import org.twelve.gcp.common.CONSTANTS;

public class Token<T> {
    private static Token unit = new Token(CONSTANTS.UNIT,0);
    public static Token unit() {
        return unit;
    }
    private final T data;
    private final Location loc;

    public Token(T data, int start) {
        this.data = data;
        this.loc = new SimpleLocation(start,data.toString().length()+start);
    }

    /**
     * system added or mocked token, not from real code
     * @param lexeme
     */
    public Token(T lexeme){
        this(lexeme,-1);
    }


    public Location loc(){
        return this.loc;
    }

    public String lexeme(){
        return this.data.toString();
    }

    public T data(){
        return this.data;
    }
}
