package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Decimal_;
import org.twelve.gcp.outline.builtin.Float_;
import org.twelve.gcp.outline.builtin.String_;

public class DECIMAL extends DOUBLE {
    private final static Decimal_ decimal = new Decimal_();

    public DECIMAL(ONode node){
        this(decimal,node);
    }
    protected DECIMAL(BuildInOutline buildInOutline,ONode node) {
        super(buildInOutline,node);
    }

}

