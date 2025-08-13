package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class Any_ extends BuildInOutline{
    public Any_() {
        super(null);
    }

    @Override
    public long id() {
        return CONSTANTS.ANY_INDEX;
    }
    @Override
    public boolean tryYouAreMe(Outline another) {
        return true;
    }
}
