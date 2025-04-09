package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class ERROR implements BuildInOutline {
    private static ERROR _instance = new ERROR();
    public static ERROR instance(){
        return _instance;
    }

    private ERROR(){
        super();
    }
    @Override
    public long id() {
        return CONSTANTS.ERROR_INDEX;
    }
}
