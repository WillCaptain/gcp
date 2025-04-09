package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.outline.builtin.Bool_;

public class BOOL extends Primitive{
    private static BOOL _instance = new BOOL();
    public static BOOL instance(){
        return _instance;
    }

    private  BOOL(){
        super(new Bool_());
    }
}
