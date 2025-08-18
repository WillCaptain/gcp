package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.builtin.Array_;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.projectable.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class Array extends DictOrArray<INTEGER> {//} implements GenericContainer {

    private Array(Node node, AST ast, Outline itemOutline) {
        super(node, ast, Array_.instance(), ast.Integer, itemOutline);
    }

    public static Array from(Node node, Outline itemOutline) {
        return new Array(node, node.ast(), itemOutline);
    }

    public static Array from(AST ast, Outline itemOutline) {
        return new Array(null, ast, itemOutline);
    }

    public Outline itemOutline() {
        return this.value;
    }

    @Override
    public boolean loadMethods() {
        if (!super.loadMethods()) return false;
        this.createMapper(this.members);
        this.createReducer(this.members);
        return true;
    }

    @Override
    public boolean inferred() {
        return this.itemOutline().inferred();
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
    public Node node() {
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
