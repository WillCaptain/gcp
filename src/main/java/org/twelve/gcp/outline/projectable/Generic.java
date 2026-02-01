package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

/**
 * outline泛化类型
 */
public class Generic extends Genericable<Generic, Node> {
    protected Generic(Node node, AST ast, Outline declaredToBe) {
        super(node, ast, declaredToBe);
    }

    public static Genericable<?, ?> from(Node node, AST ast, Outline declared) {
        if (declared instanceof Reference) return cast(declared);
        return new Generic(node, ast, declared);
    }

    public static Genericable<?, ?> from(AST ast, Outline declared) {
        return from(null, ast, declared);
    }

    public static Genericable<?, ?> from(Node node, Outline declared) {
        return from(node, node.ast(), declared);
    }

    @Override
    protected Generic createNew() {
        return new Generic(this.node, this.ast(), this.declaredToBe);
    }

    @Override
    public boolean equals(Outline another) {
        if(another instanceof Generic) {
            return this == another;
        }else{
            return super.equals(another);
        }
    }
}