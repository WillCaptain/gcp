package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

/**
 * represent null
 */
public class NOTHING implements BuildInOutline {
    private static NOTHING _instance = new NOTHING();
    public static NOTHING instance(){
        return _instance;
    }

    private NOTHING(){
        super();
    }

    @Override
    public long id() {
        return CONSTANTS.NOTHING_INDEX;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        return true;
    }

    @Override
    public boolean beAssignable() {
        return false;
    }

    @Override
    public String toString() {
        return "null";
    }
}
