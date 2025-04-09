package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Float_;

public class FLOAT extends DECIMAL{
    private final static Float_ float_ = new Float_();

    protected FLOAT(BuildInOutline buildInOutline, ONode node) {
        super(buildInOutline,node);
    }

    public FLOAT(ONode node){
        this(float_,node);
    }
}
