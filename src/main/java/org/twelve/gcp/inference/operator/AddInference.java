package org.twelve.gcp.inference.operator;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.STRING;
import org.twelve.gcp.outline.primitive.NUMBER;
import org.twelve.gcp.outline.projectable.Addable;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.OperateAble;

import static org.twelve.gcp.common.Tool.getExactNumberOutline;

public class AddInference implements OperatorInference {

    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        AST ast = node.ast();
        if (left instanceof Genericable<?,?>) {
            ((Genericable<?,?>) left).addDefinedToBe(ast.stringOrNumber(left.node()));
        }
        if (right instanceof Genericable<?,?>) {
            ((Genericable<?,?>) right).addDefinedToBe(ast.stringOrNumber(right.node()));
        }
        // ANY is the dynamic top-type (e.g. result of json() / json.parse()). Adding ANY to a
        // String always yields String; adding ANY to a Number yields Any (unknown numeric/other).
        if (left instanceof ANY || right instanceof ANY) {
            return (left instanceof STRING || right instanceof STRING) ? ast.String : ast.Any;
        }
        if ((!(left instanceof UNKNOWN) && !compatibleWithAdd(left, ast)) ||
            (!(right instanceof UNKNOWN) && !compatibleWithAdd(right, ast))) {
            return ast.Error;
        }
        if ((left instanceof UNKNOWN) || (right instanceof UNKNOWN)) {
            return new Addable(node, left, right);
        }

        if (left instanceof STRING || right instanceof STRING) return ast.String;

        if (left instanceof NUMBER && right instanceof NUMBER) {
            return left.is(right) ? right : left;
        }

        // NUMBER + non-Generic OperateAble (e.g. Addable from a recursive call's intermediate type)
        // → numeric context, same as NumOperaInference.
        // Must NOT match fresh Generic parameters (Genericable), since those could still be String
        // (e.g. `f(x) = x + 1` where x might be called with "some").
        if (left instanceof NUMBER && right instanceof OperateAble && !(right instanceof Genericable)) {
            ((OperateAble<?>) right).addDefinedToBe(ast.Number);
            return left;
        }
        if (right instanceof NUMBER && left instanceof OperateAble && !(left instanceof Genericable)) {
            ((OperateAble<?>) left).addDefinedToBe(ast.Number);
            return right;
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

    /**
     * Checks whether an outline is compatible as a `+` operand (String or Number context).
     * More lenient than a strict `is(StringOrNumber)` — recursively checks Option branches
     * so that types like Option(Int, Addable) that arise from recursive inference are accepted.
     */
    private static boolean compatibleWithAdd(Outline outline, AST ast) {
        if (outline.is(ast.StringOrNumber)) return true;
        if (outline instanceof Option opt) {
            return opt.options().stream().allMatch(o -> compatibleWithAdd(o, ast));
        }
        if (outline instanceof Addable) return true;
        return false;
    }
}
