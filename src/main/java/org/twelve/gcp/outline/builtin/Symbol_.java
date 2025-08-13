package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class Symbol_ extends BuildInOutline {
    private final String name;

    public Symbol_(String name) {
        super(null);
        this.name = name;
    }

    public String name(){
        return this.name;
    }

    @Override
    public long id() {
        return this.name.hashCode();
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if(another instanceof Symbol_){
            return this.name.equals(((Symbol_) another).name);
        }else{
            return false;
        }
    }
}
