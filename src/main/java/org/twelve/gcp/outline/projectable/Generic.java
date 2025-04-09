package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

/**
 * outline泛化类型
 */
public class Generic extends Genericable<Generic, Argument> {
    private Generic(Argument node, Outline declared) {
        super(node, declared);
    }

    public static Generic from(Outline declared) {
        return new Generic(null,declared);
    }
    public static Generic from(Argument node, Outline declared){
        return new Generic(node,declared);
    }

    @Override
    protected Generic createNew() {
        return new Generic(cast(this.node), this.declaredToBe);
    }
}