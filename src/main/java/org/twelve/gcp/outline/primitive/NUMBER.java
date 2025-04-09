package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Number_;

public class NUMBER extends Primitive {
    private static NUMBER _instance = new NUMBER();
    public static NUMBER instance(){
        return _instance;
    }

    protected NUMBER(BuildInOutline buildInOutline, ONode node){
        super(buildInOutline,node);
    }
    private  NUMBER(){
        this(new Number_(),null);
    }
}
