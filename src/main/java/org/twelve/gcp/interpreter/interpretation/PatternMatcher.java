package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.unpack.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stateless utility that implements GCP pattern-matching logic for {@code match}
 * expressions.  Extracted from {@code OutlineInterpreter} to keep each
 * {@link org.twelve.gcp.interpreter.Interpretation} class small and focused.
 */
public final class PatternMatcher {

    private PatternMatcher() {}

    /**
     * Attempts to match {@code value} against {@code pattern}, populating
     * {@code patternEnv} with any names bound by the pattern.
     *
     * @return {@code true} if the pattern matches the value
     */
    public static boolean match(
            org.twelve.gcp.node.expression.Expression pattern,
            Value value,
            Environment patternEnv,
            Interpreter interp) {

        // Wildcard _ – always matches
        if (pattern instanceof UnderLineNode) return true;

        // Literal – exact value comparison
        if (pattern instanceof LiteralNode<?> lit) {
            Value patVal = interp.eval(lit);
            return patVal.equals(value);
        }

        // Symbol identifier pattern: Male, Female, Dog, Cat...
        if (pattern instanceof SymbolIdentifier si) {
            String tag = si.name();
            if (value instanceof EntityValue ev) return tag.equals(ev.symbolTag());
            return false;
        }

        // Plain identifier – binds the whole subject to that name
        if (pattern instanceof Identifier id) {
            patternEnv.define(id.name(), value);
            return true;
        }

        // Symbol entity unpack: Male{name}
        if (pattern instanceof SymbolEntityUnpackNode seu) {
            String tag = seu.symbol().name();
            if (!(value instanceof EntityValue ev) || !tag.equals(ev.symbolTag())) return false;
            return unpackEntityPattern(seu.fields(), ev, patternEnv);
        }

        // Entity unpack: {field1, field2}
        if (pattern instanceof EntityUnpackNode eu) {
            if (!(value instanceof EntityValue ev)) return false;
            return unpackEntityPattern(eu.fields(), ev, patternEnv);
        }

        // Symbol tuple unpack: Female(name, age)
        if (pattern instanceof SymbolTupleUnpackNode stu) {
            String tag = stu.symbol().name();
            if (value instanceof EntityValue ev) {
                if (!tag.equals(ev.symbolTag())) return false;
                return unpackTupleEntityPattern(stu.begins(), stu.ends(), ev, patternEnv);
            }
            return false;
        }

        // Tuple unpack: (a, b, c)
        if (pattern instanceof TupleUnpackNode tun) {
            if (value instanceof TupleValue tv)
                return unpackTuplePattern(tun.begins(), tun.ends(), tv, patternEnv);
            if (value instanceof EntityValue ev)
                return unpackTupleEntityPattern(tun.begins(), tun.ends(), ev, patternEnv);
            return false;
        }

        // Fallback: evaluate pattern and compare
        Value patVal = interp.eval(pattern);
        return patVal.equals(value);
    }

    // -------------------------------------------------------------------------
    // Entity pattern unpacking
    // -------------------------------------------------------------------------

    public static boolean unpackEntityPattern(
            List<Field> fields, EntityValue ev, Environment patternEnv) {

        for (Field field : fields) {
            String fieldName = field.field().name();
            Value fieldVal = ev.get(fieldName);
            if (fieldVal == null) return false;

            UnpackNode nest = field.nestedUnpack();
            if (nest != null) {
                if (nest instanceof EntityUnpackNode eu2) {
                    if (!(fieldVal instanceof EntityValue nested)) return false;
                    if (!unpackEntityPattern(eu2.fields(), nested, patternEnv)) return false;
                } else if (nest instanceof TupleUnpackNode tun2) {
                    if (fieldVal instanceof TupleValue tv) {
                        if (!unpackTuplePattern(tun2.begins(), tun2.ends(), tv, patternEnv)) return false;
                    } else if (fieldVal instanceof EntityValue ev2) {
                        if (!unpackTupleEntityPattern(tun2.begins(), tun2.ends(), ev2, patternEnv)) return false;
                    } else return false;
                }
            } else {
                List<Identifier> ids = field.identifiers();
                if (!ids.isEmpty()) {
                    for (Identifier id : ids) {
                        if (!(id instanceof UnderLineNode)) patternEnv.define(id.name(), fieldVal);
                    }
                } else {
                    patternEnv.define(fieldName, fieldVal);
                }
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Tuple pattern unpacking
    // -------------------------------------------------------------------------

    public static boolean unpackTuplePattern(
            List<Node> begins, List<Node> ends, TupleValue tv, Environment patternEnv) {

        int total = tv.size();
        if (total < begins.size() + ends.size()) return false;
        for (int i = 0; i < begins.size(); i++) {
            if (!bindTuplePatternNode(begins.get(i), tv.get(i), patternEnv)) return false;
        }
        for (int i = 0; i < ends.size(); i++) {
            Value elem = tv.get(-(ends.size() - i));
            if (!bindTuplePatternNode(ends.get(i), elem, patternEnv)) return false;
        }
        return true;
    }

    private static boolean bindTuplePatternNode(Node n, Value elem, Environment patternEnv) {
        if (n instanceof UnderLineNode) return true;
        if (n instanceof Identifier id) { patternEnv.define(id.name(), elem); return true; }
        if (n instanceof TupleUnpackNode tun) {
            if (elem instanceof TupleValue inner)
                return unpackTuplePattern(tun.begins(), tun.ends(), inner, patternEnv);
            if (elem instanceof EntityValue ev)
                return unpackTupleEntityPattern(tun.begins(), tun.ends(), ev, patternEnv);
        }
        if (n instanceof EntityUnpackNode eu) {
            if (elem instanceof EntityValue ev)
                return unpackEntityPattern(eu.fields(), ev, patternEnv);
        }
        return true;
    }

    /** Unpack a symbol-tuple (stored as EntityValue with integer-named fields). */
    public static boolean unpackTupleEntityPattern(
            List<Node> begins, List<Node> ends, EntityValue ev, Environment patternEnv) {

        Map<String, Value> all = ev.allFields();
        List<Value> elems = new ArrayList<>();
        for (int i = 0; ; i++) {
            Value v = all.get(String.valueOf(i));
            if (v == null) break;
            elems.add(v);
        }
        return unpackTuplePattern(begins, ends, new TupleValue(elems), patternEnv);
    }
}
