package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Number_;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

/**
 * Number type in the GCP type system, the common supertype of all numeric types
 * (Integer, Long, Float, Double, Decimal).
 * <p>
 * Provides built-in numeric operations shared by all numeric types.
 *
 * @author huizi 2025
 */
public class NUMBER extends Primitive {
    private static Number_ number_ = new Number_();

    protected NUMBER(BuildInOutline buildInOutline, Node node, AST ast) {
        super(buildInOutline, node, ast);
    }

    public NUMBER(AST ast) {
        super(number_, null, ast);
    }

    /**
     * Loads built-in numeric methods inherited by all Number subtypes.
     * <ul>
     *   <li>{@code abs()}    : Unit → Number  — absolute value</li>
     *   <li>{@code ceil()}   : Unit → Integer — ceiling</li>
     *   <li>{@code floor()}  : Unit → Integer — floor</li>
     *   <li>{@code round()}  : Unit → Integer — round to nearest integer</li>
     *   <li>{@code to_int()} : Unit → Integer — truncate to integer</li>
     *   <li>{@code to_float()}: Unit → Double  — convert to floating-point</li>
     *   <li>{@code sqrt()}   : Unit → Double  — square root</li>
     *   <li>{@code pow(Number)}: Number → Double — exponentiation</li>
     * </ul>
     */
    @Override
    public boolean loadBuiltInMethods() {
        if (!super.loadBuiltInMethods()) return false;
        AST ast = this.ast();
        members.put("abs",      EntityMember.from("abs",      FirstOrderFunction.from(ast, ast.Number,  ast.Unit),   Modifier.PUBLIC, false, null, true));
        members.put("ceil",     EntityMember.from("ceil",     FirstOrderFunction.from(ast, ast.Integer, ast.Unit),   Modifier.PUBLIC, false, null, true));
        members.put("floor",    EntityMember.from("floor",    FirstOrderFunction.from(ast, ast.Integer, ast.Unit),   Modifier.PUBLIC, false, null, true));
        members.put("round",    EntityMember.from("round",    FirstOrderFunction.from(ast, ast.Integer, ast.Unit),   Modifier.PUBLIC, false, null, true));
        members.put("to_int",   EntityMember.from("to_int",   FirstOrderFunction.from(ast, ast.Integer, ast.Unit),   Modifier.PUBLIC, false, null, true));
        members.put("to_float", EntityMember.from("to_float", FirstOrderFunction.from(ast, ast.Double,  ast.Unit),   Modifier.PUBLIC, false, null, true));
        members.put("sqrt",     EntityMember.from("sqrt",     FirstOrderFunction.from(ast, ast.Double,  ast.Unit),   Modifier.PUBLIC, false, null, true));
        members.put("pow",      EntityMember.from("pow",      FirstOrderFunction.from(ast, ast.Double,  ast.Number), Modifier.PUBLIC, false, null, true));
        return true;
    }
}
