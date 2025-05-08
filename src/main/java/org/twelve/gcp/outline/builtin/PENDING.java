package org.twelve.gcp.outline.builtin;

import static org.twelve.gcp.common.CONSTANTS.PENDING_INDEX;

public class PENDING  implements BuildInOutline {
    private static PENDING _instance = new PENDING();
    public static PENDING instance() {
        return _instance;
    }

    private PENDING(){

    }
    @Override
    public long id() {
        return PENDING_INDEX;
    }
}
