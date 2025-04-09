package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class ANY implements BuildInOutline {
    private static ANY _instance = new ANY();
    public static ANY instance(){
        return _instance;
    }

    private ANY(){
        super();
    }
    @Override
    public boolean tryYouAreMe(Outline another) {
        return true;
    }

//    @Override
//    public boolean tryIamYou(Outline another) {
//        return true;
//    }

    @Override
    public long id() {
        return CONSTANTS.ANY_INDEX;
    }

    @Override
    public String toString() {
        return "any";
    }
}
