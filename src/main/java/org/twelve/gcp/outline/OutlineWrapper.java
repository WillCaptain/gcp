package org.twelve.gcp.outline;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;

public record OutlineWrapper(Node node, Outline outline) implements Outline {

    @Override
    public long id() {
        return CONSTANTS.OUTLINE_WRAPPER;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if(another instanceof OutlineWrapper){
            return this.outline.tryIamYou(((OutlineWrapper) another).outline);
        }else {
            return this.outline.tryIamYou(another);
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if(another instanceof  OutlineWrapper){
            return this.outline.tryYouAreMe(((OutlineWrapper) another).outline);
        }else {
            return this.outline.tryYouAreMe(another);
        }
    }

    @Override
    public boolean tryYouCanBeMe(Outline another) {
        return this.outline.tryYouCanBeMe(another);
    }

    @Override
    public String toString() {
        return outline.toString();
    }
}
