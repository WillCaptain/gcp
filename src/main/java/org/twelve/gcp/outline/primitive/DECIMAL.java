package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Decimal_;

public class DECIMAL extends DOUBLE {
    private final static Decimal_ decimal = new Decimal_();

    public DECIMAL(Node node){
        this(decimal,node);
    }
    protected DECIMAL(BuildInOutline buildInOutline, Node node) {
        super(buildInOutline,node);
    }

}

