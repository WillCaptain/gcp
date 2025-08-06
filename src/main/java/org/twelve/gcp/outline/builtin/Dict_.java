package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;

public class Dict_  implements BuildInOutline{
    private static final Dict_ _instance = new Dict_();
    public static Dict_ instance(){
        return _instance;
    }
    private Dict_(){

    }
    @Override
    public long id() {
        return CONSTANTS.DICT_INDEX;
    }
}