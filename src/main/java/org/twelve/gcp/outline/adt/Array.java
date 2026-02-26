package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.builtin.Array_;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.projectable.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class Array extends DictOrArray<INTEGER> {//} implements GenericContainer {

    private Array(AbstractNode node, AST ast, Outline itemOutline) {
        super(node, ast, Array_.instance(), ast.Integer, itemOutline);
    }

    public static Array from(AbstractNode node, Outline itemOutline) {
        return new Array(node, node.ast(), itemOutline);
    }

    public static Array from(AST ast, Outline itemOutline) {
        return new Array(null, ast, itemOutline);
    }

    public Outline itemOutline() {
        return this.value;
    }

    @Override
    public boolean loadBuiltInMethods() {
        if (!super.loadBuiltInMethods()) return false;
        this.createMapper(this.members);
        this.createReducer(this.members);
        this.createFlatMapper(this.members);
        this.createFilter(this.members);
        this.createForEach(this.members);
        this.createFind(this.members);
        this.createAny(this.members);
        this.createAll(this.members);
        this.createSort(this.members);
        members.put("len",     EntityMember.from("len",     FirstOrderFunction.from(this.ast(), this.ast().Integer,                       this.ast().Unit),    Modifier.PUBLIC, false, null, true));
        members.put("reverse", EntityMember.from("reverse", FirstOrderFunction.from(this.ast(), Array.from(this.ast(), this.itemOutline()),  this.ast().Unit),    Modifier.PUBLIC, false, null, true));
        members.put("take",    EntityMember.from("take",    FirstOrderFunction.from(this.ast(), Array.from(this.ast(), this.itemOutline()),  this.ast().Integer), Modifier.PUBLIC, false, null, true));
        members.put("drop",    EntityMember.from("drop",    FirstOrderFunction.from(this.ast(), Array.from(this.ast(), this.itemOutline()),  this.ast().Integer), Modifier.PUBLIC, false, null, true));
        members.put("concat",  EntityMember.from("concat",  FirstOrderFunction.from(this.ast(), Array.from(this.ast(), this.itemOutline()),  Array.from(this.ast(), this.itemOutline())), Modifier.PUBLIC, false, null, true));
        members.put("min",     EntityMember.from("min",     FirstOrderFunction.from(this.ast(), this.itemOutline(),                         this.ast().Unit),    Modifier.PUBLIC, false, null, true));
        members.put("max",     EntityMember.from("max",     FirstOrderFunction.from(this.ast(), this.itemOutline(),                         this.ast().Unit),    Modifier.PUBLIC, false, null, true));
        return true;
    }

    @Override
    public boolean inferred() {
        return this.itemOutline().inferred();
    }

    /**
     * flat_map: (T → [T]) → [T]
     * Applies a mapper that returns an array and flattens one level.
     * Argument type is the concrete function T→[T]; no Generic wrapper is used because
     * both T values are the same known element type, so no new type variable is needed.
     */
    private void createFlatMapper(Map<String, EntityMember> members) {
        AST ast = this.ast();
        // ret_flat is an unconstrained Reference used as the inner function's return type.
        // Using a Reference here allows any Array-returning lambda to pass the compatibility check
        // (Reference.emptyConstraint() == true) while the outer return stays as [T].
        Identifier mock_u = new Identifier(ast, new Token<>("u"));
        Reference ret_flat = Reference.from(new ReferenceNode(mock_u, null), null);
        // flatMapper: T -> ret_flat  (any-array return, accepted by compatibility check)
        FirstOrderFunction flatMapper = FirstOrderFunction.from(ast, ret_flat, this.itemOutline());
        Generic arg = cast(Generic.from(ast, flatMapper));
        // Outer return: Array(T) — concrete, like filter's outer return
        Returnable ret = Return.from(ast);
        ret.addReturn(Array.from(ast, this.itemOutline()));
        members.put("flat_map", EntityMember.from("flat_map", FirstOrderFunction.from(ast, arg, ret, new ArrayList<>()), Modifier.PUBLIC, false, null, true));
    }

    /**
     * filter: (T → Bool) → [T]
     * Retains only elements satisfying the predicate.
     */
    private void createFilter(Map<String, EntityMember> members) {
        AST ast = this.ast();
        FirstOrderFunction predicate = FirstOrderFunction.from(ast, ast.Boolean, this.itemOutline());
        Generic arg = cast(Generic.from(ast, predicate));
        Returnable ret = Return.from(ast);
        ret.addReturn(Array.from(ast, this.itemOutline()));
        members.put("filter", EntityMember.from("filter", FirstOrderFunction.from(ast, arg, ret, new ArrayList<>()), Modifier.PUBLIC, false, null, true));
    }

    /**
     * forEach: (T → Any) → Unit
     * Calls the consumer for each element; return value of consumer is ignored.
     */
    private void createForEach(Map<String, EntityMember> members) {
        AST ast = this.ast();
        Identifier mock_b = new Identifier(ast, new Token<>("b"));
        Reference consumer_ret = Reference.from(new ReferenceNode(mock_b, null), null);
        FirstOrderFunction consumer = FirstOrderFunction.from(ast, consumer_ret, this.itemOutline());
        Generic arg = cast(Generic.from(ast, consumer));
        List<Reference> refs = new ArrayList<>();
        refs.add(consumer_ret);
        Returnable ret = Return.from(ast);
        ret.addReturn(ast.Unit);
        members.put("forEach", EntityMember.from("forEach", FirstOrderFunction.from(ast, arg, ret, refs), Modifier.PUBLIC, false, null, true));
    }

    /**
     * find: (T → Bool) → T
     * Returns the first element matching the predicate.
     */
    private void createFind(Map<String, EntityMember> members) {
        AST ast = this.ast();
        FirstOrderFunction predicate = FirstOrderFunction.from(ast, ast.Boolean, this.itemOutline());
        Generic arg = cast(Generic.from(ast, predicate));
        Returnable ret = Return.from(ast);
        ret.addReturn(this.itemOutline());
        members.put("find", EntityMember.from("find", FirstOrderFunction.from(ast, arg, ret, new ArrayList<>()), Modifier.PUBLIC, false, null, true));
    }

    /**
     * any: (T → Bool) → Bool
     * Returns true if at least one element satisfies the predicate.
     */
    private void createAny(Map<String, EntityMember> members) {
        AST ast = this.ast();
        FirstOrderFunction predicate = FirstOrderFunction.from(ast, ast.Boolean, this.itemOutline());
        Generic arg = cast(Generic.from(ast, predicate));
        Returnable ret = Return.from(ast);
        ret.addReturn(ast.Boolean);
        members.put("any", EntityMember.from("any", FirstOrderFunction.from(ast, arg, ret, new ArrayList<>()), Modifier.PUBLIC, false, null, true));
    }

    /**
     * all: (T → Bool) → Bool
     * Returns true if every element satisfies the predicate.
     */
    private void createAll(Map<String, EntityMember> members) {
        AST ast = this.ast();
        FirstOrderFunction predicate = FirstOrderFunction.from(ast, ast.Boolean, this.itemOutline());
        Generic arg = cast(Generic.from(ast, predicate));
        Returnable ret = Return.from(ast);
        ret.addReturn(ast.Boolean);
        members.put("all", EntityMember.from("all", FirstOrderFunction.from(ast, arg, ret, new ArrayList<>()), Modifier.PUBLIC, false, null, true));
    }

    /**
     * sort: (T → T → Number) → [T]
     * Returns a new sorted array using the given comparator (curried: negative/zero/positive).
     * The comparator returns Number to accept any numeric difference (integer or floating-point).
     */
    private void createSort(Map<String, EntityMember> members) {
        AST ast = this.ast();
        FirstOrderFunction comparator = FirstOrderFunction.from(ast, ast.Number, this.itemOutline(), this.itemOutline());
        Generic arg = cast(Generic.from(ast, comparator));
        Returnable ret = Return.from(ast);
        ret.addReturn(Array.from(ast, this.itemOutline()));
        members.put("sort", EntityMember.from("sort", FirstOrderFunction.from(ast, arg, ret, new ArrayList<>()), Modifier.PUBLIC, false, null, true));
    }

    private void createReducer(Map<String, EntityMember> members) {
        AST ast = this.ast();
        Identifier mock_a = new Identifier(this.ast(), new Token<>("a"));
        Reference ret_of_reduce = Reference.from(new ReferenceNode(mock_a, null), null);
        FirstOrderFunction reduce = FirstOrderFunction.from(this.ast(), ret_of_reduce, ret_of_reduce, this.itemOutline());
//        Generic arg = cast(Generic.from(ast, reduce));
        List<Reference> refs = new ArrayList<>();
        refs.add(ret_of_reduce);
        Returnable ret = Return.from(ast);
        ret.addReturn(ret_of_reduce);
        FirstOrderFunction reducer = FirstOrderFunction.from(ast, refs, ret,reduce, ret_of_reduce);
        members.put("reduce", EntityMember.from("reduce", reducer, Modifier.PUBLIC, false, null, true));
    }

    private void createMapper(Map<String, EntityMember> members) {
        AST ast = this.ast();
        Identifier mock_a = new Identifier(this.ast(), new Token<>("a"));
        Reference ret_of_map = Reference.from(new ReferenceNode(mock_a, null), null);//the return reference of map
        FirstOrderFunction map = FirstOrderFunction.from(this.ast(), ret_of_map, this.itemOutline());//input array item, output ret_of_map
        Generic arg = cast(Generic.from(ast, map));
        List<Reference> refs = new ArrayList<>();
        refs.add(ret_of_map);
        Returnable ret = Return.from(ast);
        ret.addReturn(Array.from(ast, ret_of_map));
        FirstOrderFunction mapper = FirstOrderFunction.from(ast, arg, ret, refs);
        members.put("map", EntityMember.from("map", mapper, Modifier.PUBLIC, false, null, true));
    }


    @Override
    public AbstractNode node() {
        return this.node;
    }

    @Override
    public String toString() {
        return "[" + value + "]";
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        if (reference.id() == this.value.id()) {
            return Array.from(this.node, projection.outline());
        } else {
            return this;
        }
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (projected.id() == this.id()) {
            if (!projection.is(this)) {
                GCPErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
                return this.guess();
            }
            Array you = cast(projection);
            if (this.value instanceof Projectable) {
                ((Projectable) this.value).project(cast(this.value), you.value, session);
            }
            return projection;
        } else {
            if (this.value instanceof Projectable) {
                return new Array(this.node, this.ast(), ((Projectable) this.value).project(projected, projection, session));
            } else {
                return this;
            }
        }
    }

    @Override
    public Outline guess() {
        if (this.value instanceof Projectable) {
            return new Array(this.node, this.ast(), ((Projectable) this.value).guess());
        } else {
            return this;
        }
    }

    @Override
    public Array copy(Map<Outline, Outline> cache) {
        Array copied = cast(cache.get(this));
        if (copied == null) {
            copied = new Array(this.node, this.ast(), this.value.copy(cache));
            cache.put(this, copied);
        }
        return copied;
    }

    @Override
    public boolean beAssignable() {
        return this.value.beAssignable();
    }

    @Override
    public Array alternative() {
        if (this.value instanceof NOTHING) {
            return Array.from(this.node, this.node.ast().Any);
        } else {
            return this;
        }
    }
}
