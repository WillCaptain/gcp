package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;

import static org.twelve.gcp.common.Tool.cast;

/**
 * outline泛化类型
 */
public class Generic extends Genericable<Generic, Node> {
    //if(x is A as y){...}
//    protected Outline couldBe = Any;
    private Generic(Node node, Outline declared) {
        super(node, declared);
    }

    public static Genericable<?,?> from(Outline declared) {
        if(declared instanceof Reference) return cast(declared);
        return new Generic(null,declared);
    }
    public static Genericable<?,?> from(Node node, Outline declared){
        if(declared instanceof Reference) return cast(declared);
        return new Generic(node,declared);
    }

    @Override
    protected Generic createNew() {
        return new Generic(cast(this.node), this.declaredToBe);
    }

//    public void addCouldBe(Outline outline){
//        if(couldBe==Any){
//            couldBe = outline;
//        }else{
//            couldBe = Option.from(this.node,couldBe,outline);
//        }
//    }
//    public Outline couldBe() {
//        return couldBe;
//    }
}