package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

/**
 * outline before type inference
 */
public class UNKNOWN implements BuildInOutline {
//    private static UNKNOWN _instance = new UNKNOWN();
//    public static UNKNOWN instance(){
//        return _instance;
//    }

    public UNKNOWN(){
        super();
    }
    @Override
    public boolean tryYouAreMe(Outline another) {
        return true;
    }

    @Override
    public long id() {
        return CONSTANTS.UNKNOWN_INDEX;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        return false;
    }

    @Override
    public String toString() {
        return "?";
    }
}
