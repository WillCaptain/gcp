package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.outline.primitive.Literal;

public class MemberAccessorInterpretation implements Interpretation<MemberAccessor> {
    @Override
    public Value interpret(MemberAccessor node, Interpreter interp) {
        Value target = interp.eval(node.host());
        String memberName = node.member().name();

        if (target instanceof EntityValue entity) {
            Value member = entity.get(memberName);
            if (member == null) {
                // Literal-type fields are not stored in the entity value;
                // their value is implicitly the literal constant defined in the outline.
                // Use the MemberAccessor node's own outline (member identifier outline stays UNKNOWN).
                var memberOutline = node.outline();
                if (memberOutline instanceof Literal lit && lit.node() instanceof LiteralNode<?> ln) {
                    return interp.eval(ln);
                }
                throw new RuntimeException("Member '" + memberName + "' not found on " + entity);
            }
            if (member instanceof FunctionValue fv && !fv.isBuiltin()) {
                Environment methodEnv = new Environment(fv.closure());
                methodEnv.define("this", entity);
                return new FunctionValue(fv.node(), methodEnv);
            }
            return member;
        }

        if (target instanceof TupleValue tv) {
            try {
                int idx = Integer.parseInt(memberName);
                Value elem = tv.get(idx);
                if (elem instanceof FunctionValue fv && !fv.isBuiltin()) {
                    Environment methodEnv = new Environment(fv.closure());
                    methodEnv.define("this", tv);
                    return new FunctionValue(fv.node(), methodEnv);
                }
                return elem;
            } catch (NumberFormatException ignored) {}
            return BuiltinMethods.tuple(tv, memberName);
        }

        if (target instanceof ArrayValue arr)  return BuiltinMethods.array(arr, memberName, interp);
        if (target instanceof StringValue sv)  return BuiltinMethods.string(sv, memberName);
        if (target instanceof IntValue iv)     return BuiltinMethods.integer(iv, memberName);
        if (target instanceof FloatValue fv)   return BuiltinMethods.floatingPoint(fv, memberName);
        if (target instanceof BoolValue bv)    return BuiltinMethods.bool(bv, memberName);
        if (target instanceof DictValue dv)    return BuiltinMethods.dict(dv, memberName);

        throw new RuntimeException("Cannot access member '" + memberName + "' on " + target);
    }
}
