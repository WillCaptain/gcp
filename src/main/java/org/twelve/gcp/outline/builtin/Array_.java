package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;

public class Array_ extends BuildInOutline{
    private static final Array_ _instance = new Array_();
    public static Array_ instance(){
        return _instance;
    }
    private Array_(){
        super(null);
    }
    @Override
    public long id() {
        return CONSTANTS.ARRAY_INDEX;
    }
}
