package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Any_;
import org.twelve.gcp.outline.builtin.Nothing_;

import static org.twelve.gcp.common.Tool.cast;

/**
 * literal type, a value type
 */
public class Literal extends Primitive {
    private final Outline origin;

    public Literal(ValueNode node, Outline outline, AST ast) {
        super(new Any_(), node, ast);
        this.origin = outline;
    }

    @Override
    public String toString() {
        return this.origin.toString();
    }

    @Override
    public boolean is(Outline another) {
        if(another instanceof Literal){
            return this.origin.is(((Literal) another).origin) && ((ValueNode<?>) another.node()).isSame(cast(this.node()));
        }else{
            return super.is(another);
        }
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another instanceof Literal) {
            return another.tryYouAreMe(this);
        } else {
            return this.origin.is(another);
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (!another.is(this.origin)) return false;
        if (!(another.node() instanceof ValueNode<?>)) return false;
        return ((ValueNode<?>) another.node()).isSame(cast(this.node()));
//        if(!(another instanceof Literal)) return false;
//        if(!((Literal)another).origin.is(this.origin)) return false;
//        return ((LiteralNode)this.node()).isSame(cast(another.node()));
    }

    public Outline outline() {
        return this.origin;
    }
}
