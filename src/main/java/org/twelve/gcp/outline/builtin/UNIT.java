package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class UNIT implements BuildInOutline {
    private static UNIT _instance = new UNIT();
    public static UNIT instance(){
        return _instance;
    }

    private UNIT(){
        super();
    }

    @Override
    public boolean is(Outline another) {
        return another instanceof UNIT;
    }

    @Override
    public boolean equals(Outline another) {
        return another instanceof UNIT;
    }

    @Override
    public long id() {
        return CONSTANTS.UNIT_INDEX;
    }

    @Override
    public String toString() {
        return "()";
    }
}
