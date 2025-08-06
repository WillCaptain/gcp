package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Float_;

public class FLOAT extends DECIMAL{
    private final static Float_ float_ = new Float_();
    public static FLOAT create(Node node){
        FLOAT f = new FLOAT(node);
        f.init();
        return f;
    }
    protected FLOAT(BuildInOutline buildInOutline, Node node) {
        super(buildInOutline,node);
    }

    private FLOAT(Node node){
        this(float_,node);
    }
    public FLOAT(){
        this(null);
    }
}
