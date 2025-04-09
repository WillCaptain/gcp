package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;

public class Decimal_ extends Double_ {
    @Override
    public long id() {
        return CONSTANTS.DECIMAL_INDEX;
    }
}