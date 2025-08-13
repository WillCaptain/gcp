package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class Nothing_ extends BuildInOutline{
    public Nothing_() {
        super(null);
    }

    @Override
    public long id() {
        return CONSTANTS.NOTHING_INDEX;
    }
    @Override
    public boolean tryIamYou(Outline another) {
        return true;
    }
}
