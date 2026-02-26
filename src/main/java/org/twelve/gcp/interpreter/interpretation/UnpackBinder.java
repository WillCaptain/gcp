package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.unpack.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility that handles destructuring (unpack) assignments.
 * Used by {@link AssignmentInterpretation} and {@link VariableDeclaratorInterpretation}.
 */
public final class UnpackBinder {

    private UnpackBinder() {}

    // -------------------------------------------------------------------------
    // Tuple destructuring
    // -------------------------------------------------------------------------

    public static void unpackTuple(TupleUnpackNode node, Value value, Interpreter interp) {
        List<Value> elems;
        if (value instanceof TupleValue tv) {
            elems = tv.elements();
        } else if (value instanceof EntityValue ev) {
            elems = new ArrayList<>();
            for (int i = 0; ; i++) {
                Value v = ev.get(String.valueOf(i));
                if (v == null) break;
                elems.add(v);
            }
        } else {
            throw new RuntimeException("Cannot tuple-unpack from: " + value);
        }

        List<Node> begins = node.begins();
        List<Node> ends   = node.ends();
        for (int i = 0; i < begins.size(); i++) {
            Value elem = i < elems.size() ? elems.get(i) : UnitValue.INSTANCE;
            bindNode(begins.get(i), elem, interp);
        }
        for (int i = 0; i < ends.size(); i++) {
            int j = elems.size() - ends.size() + i;
            Value elem = j >= 0 && j < elems.size() ? elems.get(j) : UnitValue.INSTANCE;
            bindNode(ends.get(i), elem, interp);
        }
    }

    // -------------------------------------------------------------------------
    // Entity destructuring
    // -------------------------------------------------------------------------

    public static void unpackEntity(EntityUnpackNode node, Value value, Interpreter interp) {
        if (!(value instanceof EntityValue ev)) {
            throw new RuntimeException("Cannot entity-unpack from: " + value);
        }
        for (Field field : node.fields()) {
            String fieldName = field.field().name();
            Value fieldVal = ev.get(fieldName);
            if (fieldVal == null) fieldVal = UnitValue.INSTANCE;
            UnpackNode nest = field.nestedUnpack();
            if (nest != null) {
                if (nest instanceof EntityUnpackNode eu2) unpackEntity(eu2, fieldVal, interp);
                else if (nest instanceof TupleUnpackNode tun2) unpackTuple(tun2, fieldVal, interp);
            } else {
                List<Identifier> ids = field.identifiers();
                if (!ids.isEmpty()) {
                    for (Identifier id : ids) {
                        if (!(id instanceof UnderLineNode)) interp.env().define(id.name(), fieldVal);
                    }
                } else {
                    interp.env().define(fieldName, fieldVal);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // LHS binding for assignment targets
    // -------------------------------------------------------------------------

    public static void bindAssignable(
            org.twelve.gcp.node.expression.Assignable lhs, Value value, Interpreter interp) {

        if (lhs instanceof TupleUnpackNode tun)  { unpackTuple(tun, value, interp); return; }
        if (lhs instanceof EntityUnpackNode eun) { unpackEntity(eun, value, interp); return; }

        if (lhs instanceof org.twelve.gcp.node.expression.Variable v) {
            org.twelve.gcp.node.expression.Assignable inner = v.identifier();
            if (inner instanceof TupleUnpackNode tun)  { unpackTuple(tun, value, interp); return; }
            if (inner instanceof EntityUnpackNode eun) { unpackEntity(eun, value, interp); return; }
            String name = v.name();
            Boolean mutable = v.mutable();
            if (mutable == null || !mutable) {
                interp.env().define(name, value);
            } else {
                interp.env().set(name, value);
            }
            return;
        }

        if (lhs instanceof Identifier id) {
            interp.env().set(id.name(), value);
            return;
        }

        if (lhs instanceof org.twelve.gcp.node.expression.accessor.MemberAccessor ma) {
            Value target = interp.eval(ma.host());
            if (target instanceof EntityValue ev) {
                ev.setField(ma.member().name(), value);
                return;
            }
        }

        throw new RuntimeException("Cannot assign to: " + lhs.getClass().getSimpleName());
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    private static void bindNode(Node n, Value value, Interpreter interp) {
        if (n instanceof UnderLineNode) return;
        if (n instanceof Identifier id) { interp.env().define(id.name(), value); return; }
        if (n instanceof TupleUnpackNode tun)  { unpackTuple(tun, value, interp); return; }
        if (n instanceof EntityUnpackNode eun) { unpackEntity(eun, value, interp); }
    }
}
