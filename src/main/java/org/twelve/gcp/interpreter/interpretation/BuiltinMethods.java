package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static factory: builds built-in method {@link FunctionValue}s for primitive and
 * collection runtime values.  Mirrors the helper methods previously inlined in
 * {@code OutlineInterpreter} but decoupled so every {@link org.twelve.gcp.interpreter.Interpretation}
 * can call them via the shared {@link Interpreter} context.
 */
public final class BuiltinMethods {

    private BuiltinMethods() {}

    // -------------------------------------------------------------------------
    // Array
    // -------------------------------------------------------------------------

    public static Value array(ArrayValue arr, String method, Interpreter interp) {
        return switch (method) {
            case "len", "size", "count" -> new FunctionValue(u -> new IntValue(arr.size()));
            case "first"   -> new FunctionValue(u -> arr.size() > 0 ? arr.get(0) : UnitValue.INSTANCE);
            case "last"    -> new FunctionValue(u -> arr.size() > 0 ? arr.get(arr.size() - 1) : UnitValue.INSTANCE);
            case "reverse" -> new FunctionValue(u -> new ArrayValue(reversed(arr.elements())));
            case "to_str"  -> new FunctionValue(u -> new StringValue(arr.display()));
            case "min"     -> new FunctionValue(u -> arr.elements().stream()
                    .min((a, b) -> Double.compare(toDouble(a), toDouble(b))).orElse(UnitValue.INSTANCE));
            case "max"     -> new FunctionValue(u -> arr.elements().stream()
                    .max((a, b) -> Double.compare(toDouble(a), toDouble(b))).orElse(UnitValue.INSTANCE));
            case "map"      -> new FunctionValue(fn -> new ArrayValue(
                    arr.elements().stream().map(e -> interp.apply(fn, e)).collect(Collectors.toList())));
            case "flat_map" -> new FunctionValue(fn -> {
                List<Value> out = new ArrayList<>();
                for (Value e : arr.elements()) {
                    Value r = interp.apply(fn, e);
                    if (r instanceof ArrayValue av) out.addAll(av.elements());
                    else out.add(r);
                }
                return new ArrayValue(out);
            });
            case "filter"  -> new FunctionValue(fn -> new ArrayValue(
                    arr.elements().stream().filter(e -> interp.apply(fn, e).isTruthy()).collect(Collectors.toList())));
            case "forEach" -> new FunctionValue(fn -> {
                arr.elements().forEach(e -> interp.apply(fn, e));
                return UnitValue.INSTANCE;
            });
            case "any"     -> new FunctionValue(fn -> BoolValue.of(
                    arr.elements().stream().anyMatch(e -> interp.apply(fn, e).isTruthy())));
            case "all"     -> new FunctionValue(fn -> BoolValue.of(
                    arr.elements().stream().allMatch(e -> interp.apply(fn, e).isTruthy())));
            case "find"    -> new FunctionValue(fn -> arr.elements().stream()
                    .filter(e -> interp.apply(fn, e).isTruthy()).findFirst().orElse(UnitValue.INSTANCE));
            case "sort"    -> new FunctionValue(cmp -> {
                List<Value> sorted = new ArrayList<>(arr.elements());
                sorted.sort((a, b) -> (int) toLong(interp.apply(interp.apply(cmp, a), b)));
                return new ArrayValue(sorted);
            });
            case "take"    -> new FunctionValue(n -> {
                int cnt = (int) toLong(n);
                return new ArrayValue(arr.elements().subList(0, Math.min(cnt, arr.size())));
            });
            case "drop"    -> new FunctionValue(n -> {
                int cnt = (int) toLong(n);
                return new ArrayValue(arr.elements().subList(Math.min(cnt, arr.size()), arr.size()));
            });
            case "reduce"  -> new FunctionValue(fn -> new FunctionValue(init -> {
                Value acc = init;
                for (Value e : arr.elements()) acc = interp.apply(interp.apply(fn, acc), e);
                return acc;
            }));
            case "concat"  -> new FunctionValue(other -> {
                List<Value> out = new ArrayList<>(arr.elements());
                if (other instanceof ArrayValue av) out.addAll(av.elements());
                return new ArrayValue(out);
            });
            default -> throw new RuntimeException("Unknown array method: " + method);
        };
    }

    // -------------------------------------------------------------------------
    // String
    // -------------------------------------------------------------------------

    public static Value string(StringValue sv, String method) {
        String s = sv.value();
        return switch (method) {
            case "len", "size", "length" -> new FunctionValue(u -> new IntValue(s.length()));
            case "to_str"      -> new FunctionValue(u -> sv);
            case "trim"        -> new FunctionValue(u -> new StringValue(s.trim()));
            case "to_upper"    -> new FunctionValue(u -> new StringValue(s.toUpperCase()));
            case "to_lower"    -> new FunctionValue(u -> new StringValue(s.toLowerCase()));
            case "to_int"      -> new FunctionValue(u -> {
                try { return new IntValue(Long.parseLong(s.trim())); }
                catch (NumberFormatException e) { return UnitValue.INSTANCE; }
            });
            case "to_number"   -> new FunctionValue(u -> {
                try { return new FloatValue(Double.parseDouble(s.trim())); }
                catch (NumberFormatException e) { return UnitValue.INSTANCE; }
            });
            case "chars"       -> new FunctionValue(u -> new ArrayValue(
                    s.chars().mapToObj(c -> (Value) new StringValue(String.valueOf((char) c)))
                            .collect(Collectors.toList())));
            case "split"       -> new FunctionValue(sep -> {
                String delim = ((StringValue) sep).value();
                return new ArrayValue(Arrays.stream(s.split(delim, -1))
                        .map(part -> (Value) new StringValue(part)).collect(Collectors.toList()));
            });
            case "contains"    -> new FunctionValue(sub -> BoolValue.of(s.contains(((StringValue) sub).value())));
            case "starts_with" -> new FunctionValue(pre -> BoolValue.of(s.startsWith(((StringValue) pre).value())));
            case "ends_with"   -> new FunctionValue(suf -> BoolValue.of(s.endsWith(((StringValue) suf).value())));
            case "index_of"    -> new FunctionValue(sub -> new IntValue(s.indexOf(((StringValue) sub).value())));
            case "sub_str"     -> new FunctionValue(start -> new FunctionValue(end ->
                    new StringValue(s.substring((int) toLong(start), (int) toLong(end)))));
            case "replace"     -> new FunctionValue(old -> new FunctionValue(neo ->
                    new StringValue(s.replace(((StringValue) old).value(), ((StringValue) neo).value()))));
            case "repeat"      -> new FunctionValue(n -> new StringValue(s.repeat((int) toLong(n))));
            default -> throw new RuntimeException("Unknown string method: " + method);
        };
    }

    // -------------------------------------------------------------------------
    // Int
    // -------------------------------------------------------------------------

    public static Value integer(IntValue iv, String method) {
        long n = iv.value();
        return switch (method) {
            case "to_str"   -> new FunctionValue(u -> new StringValue(String.valueOf(n)));
            case "abs"      -> new FunctionValue(u -> new IntValue(Math.abs(n)));
            case "ceil"     -> new FunctionValue(u -> new IntValue(n));
            case "floor"    -> new FunctionValue(u -> new IntValue(n));
            case "round"    -> new FunctionValue(u -> new IntValue(n));
            case "to_int"   -> new FunctionValue(u -> iv);
            case "to_float" -> new FunctionValue(u -> new FloatValue((double) n));
            case "sqrt"     -> new FunctionValue(u -> new FloatValue(Math.sqrt(n)));
            case "pow"      -> new FunctionValue(exp -> new FloatValue(Math.pow(n, toDouble(exp))));
            case "min"      -> new FunctionValue(other -> toDouble(other) < n ? other : iv);
            case "max"      -> new FunctionValue(other -> toDouble(other) > n ? other : iv);
            default -> throw new RuntimeException("Unknown int method: " + method);
        };
    }

    // -------------------------------------------------------------------------
    // Float
    // -------------------------------------------------------------------------

    public static Value floatingPoint(FloatValue fv, String method) {
        double d = fv.value();
        return switch (method) {
            case "to_str"   -> new FunctionValue(u -> new StringValue(String.valueOf(d)));
            case "abs"      -> new FunctionValue(u -> new FloatValue(Math.abs(d)));
            case "ceil"     -> new FunctionValue(u -> new IntValue((long) Math.ceil(d)));
            case "floor"    -> new FunctionValue(u -> new IntValue((long) Math.floor(d)));
            case "round"    -> new FunctionValue(u -> new IntValue(Math.round(d)));
            case "to_int"   -> new FunctionValue(u -> new IntValue((long) d));
            case "to_float" -> new FunctionValue(u -> fv);
            case "sqrt"     -> new FunctionValue(u -> new FloatValue(Math.sqrt(d)));
            case "pow"      -> new FunctionValue(exp -> new FloatValue(Math.pow(d, toDouble(exp))));
            case "min"      -> new FunctionValue(other -> toDouble(other) < d ? other : fv);
            case "max"      -> new FunctionValue(other -> toDouble(other) > d ? other : fv);
            default -> throw new RuntimeException("Unknown float method: " + method);
        };
    }

    // -------------------------------------------------------------------------
    // Bool
    // -------------------------------------------------------------------------

    public static Value bool(BoolValue bv, String method) {
        boolean b = bv.isTruthy();
        return switch (method) {
            case "to_str"   -> new FunctionValue(u -> new StringValue(String.valueOf(b)));
            case "not"      -> new FunctionValue(u -> BoolValue.of(!b));
            case "and_also" -> new FunctionValue(other -> BoolValue.of(b && other.isTruthy()));
            case "or_else"  -> new FunctionValue(other -> BoolValue.of(b || other.isTruthy()));
            default -> throw new RuntimeException("Unknown bool method: " + method);
        };
    }

    // -------------------------------------------------------------------------
    // Dict
    // -------------------------------------------------------------------------

    public static Value dict(DictValue dv, String method) {
        return switch (method) {
            case "len", "size", "count" -> new FunctionValue(u -> new IntValue(dv.size()));
            case "to_str"       -> new FunctionValue(u -> new StringValue(dv.display()));
            case "keys"         -> new FunctionValue(u -> new ArrayValue(new ArrayList<>(dv.keys())));
            case "values"       -> new FunctionValue(u -> new ArrayValue(new ArrayList<>(dv.values())));
            case "contains_key" -> new FunctionValue(k -> BoolValue.of(dv.containsKey(k)));
            case "get"          -> new FunctionValue(k -> {
                Value v = dv.get(k);
                return v != null ? v : UnitValue.INSTANCE;
            });
            case "put"          -> new FunctionValue(k -> new FunctionValue(v -> {
                dv.put(k, v);
                return dv;
            }));
            default -> throw new RuntimeException("Unknown dict method: " + method);
        };
    }

    // -------------------------------------------------------------------------
    // Tuple
    // -------------------------------------------------------------------------

    public static Value tuple(TupleValue tv, String method) {
        return switch (method) {
            case "len", "size" -> new FunctionValue(u -> new IntValue(tv.size()));
            case "to_str"      -> new FunctionValue(u -> new StringValue(tv.display()));
            default -> throw new RuntimeException("Unknown tuple method: " + method);
        };
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    public static double toDouble(Value v) {
        if (v instanceof IntValue iv)   return (double) iv.value();
        if (v instanceof FloatValue fv) return fv.value();
        throw new RuntimeException("Expected numeric value, got: " + v);
    }

    public static long toLong(Value v) {
        if (v instanceof IntValue iv)   return iv.value();
        if (v instanceof FloatValue fv) return (long) fv.value();
        throw new RuntimeException("Expected integer, got: " + v);
    }

    private static List<Value> reversed(List<Value> list) {
        List<Value> r = new ArrayList<>(list);
        Collections.reverse(r);
        return r;
    }
}
