package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class Number_ extends BuildInOutline {
    public Number_() {
        super(null);
    }

    @Override
    public long id() {
        return CONSTANTS.NUMBER_INDEX;
    }
}
