package org.twelve.gcp.inference.operator;

import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.STRING;
import org.twelve.gcp.outline.primitive.NUMBER;
import org.twelve.gcp.outline.projectable.Addable;
import org.twelve.gcp.outline.projectable.Generic;

public class AddInference implements OperatorInference {

    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        if (left instanceof Generic) {
            ((Generic) left).addDefinedToBe(Option.StringOrNumber);
        }
        if (right instanceof Generic) {
            ((Generic) right).addDefinedToBe(Option.StringOrNumber);
        }
        if ((!(left instanceof UNKNOWN) && !left.is(Option.StringOrNumber)) || (!(right instanceof UNKNOWN) && !right.is(Option.StringOrNumber))) {
            return Outline.Error;
        }
        if ((left instanceof UNKNOWN) || (right instanceof UNKNOWN)) {
            return new Addable(node, left, right);
        }

        if (left instanceof STRING || right instanceof STRING) return Outline.String;

        if (left instanceof NUMBER && right instanceof NUMBER) {
            return left.is(right) ? right : left;
        }
        return new Addable(node, left, right);

//        //left and right is number or string
//        if ((left instanceof STRING && right instanceof STRING)
//                || (left instanceof STRING && right.is(Option.StringOrNumber))
//                || (left.is(Option.StringOrNumber) && right instanceof STRING)) {
//            return Outline.String;
//        }
//        if (left instanceof NUMBER && right instanceof NUMBER) {
//            return left.is(right) ? right : left;
//        }
//        //left and right is string or generic
//        if (left.is(Option.StringOrNumber) && right instanceof Generic) {
//            ((Generic) right).addDefinedToBe(Option.StringOrNumber);
//            return new Addable(node, left, right);
//        }
//        if (right.is(Option.StringOrNumber) && left instanceof Generic) {
//            ((Generic) left).addDefinedToBe(Option.StringOrNumber);
//            return new Addable(node, left, right);
//        }
//
//        //left and right are both generic
//        if (left instanceof Generic && right instanceof Generic) {
//            return new Addable(node, left, right);
//        }
//
//        if((left instanceof UNKNOWN) || (right instanceof UNKNOWN)){
//            return Outline.Unknown;
//        }
//
//        return Outline.Error;
    }
}
