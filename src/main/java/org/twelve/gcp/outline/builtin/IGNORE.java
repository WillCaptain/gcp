package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class IGNORE implements BuildInOutline {
    private static IGNORE _instance = new IGNORE();
    public static IGNORE instance(){
        return _instance;
    }

    private IGNORE(){
        super();
    }
    @Override
    public boolean is(Outline another) {
        return false;
    }

    @Override
    public long id() {
        return CONSTANTS.IGNORE_INDEX;
    }

    @Override
    public boolean equals(Outline another) {
        return another instanceof IGNORE;
    }

}
