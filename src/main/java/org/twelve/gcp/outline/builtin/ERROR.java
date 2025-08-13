package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;

public class ERROR extends BuildInOutline{
    public ERROR(AST ast) {
        super(ast);
    }

    @Override
    public long id() {
        return CONSTANTS.ERROR_INDEX;
    }
}