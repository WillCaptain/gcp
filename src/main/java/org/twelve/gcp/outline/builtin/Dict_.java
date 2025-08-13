package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;

public class Dict_  extends BuildInOutline{
    private static final Dict_ _instance = new Dict_();
    public static Dict_ instance(){
        return _instance;
    }
    private Dict_(){
        super(null);
    }
    @Override
    public long id() {
        return CONSTANTS.DICT_INDEX;
    }
}