package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.TupleMatcher;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.UnderLineNode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Tuple;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class TupleUnpackNode extends UnpackNode {
    protected final List<Node> begins;
    protected final List<Node> ends;

    public TupleUnpackNode(AST ast, List<Node> begins, List<Node> ends) {
        super(ast, null);
        this.begins = begins;
        for (Node begin : this.begins) {
            this.addNode(begin);
        }
        this.ends = ends;
        for (Node end : this.ends) {
            this.addNode(end);
        }
        this.outline = ast.unknown(this);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(begins.stream().map(Object::toString).collect(Collectors.joining(", ")));
        if (!ends.isEmpty()) {
            str.append(", ..., ").append(ends.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        return "(" + str + ")";

    }

    @Override
    public Outline accept(Inferences inferences) {
        super.accept(inferences);
        return inferences.visit(this);
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if ((inferred instanceof Tuple)) {
            if (!(inferred.is(this.outline()))) {
                GCPErrorReporter.report(ast(), this, GCPErrCode.OUTLINE_MISMATCH, inferred + " is not unpackable");
                return;
            }
            Tuple tuple = cast(inferred);
            TupleMatcher matcher = new TupleMatcher(tuple);
            for (int i = 0; i < this.begins.size(); i++) {
                Node node = this.begins.get(i);
                if (node instanceof Assignable) {
                    ((Assignable) node).assign(env, matcher.match(i));
                }
            }

            for (int i = 0; i < this.ends.size(); i++) {
                Node node = this.ends.get(i);
                int j = i - this.ends.size();
                if (node instanceof Assignable) {
                    ((Assignable) node).assign(env, matcher.match(j));
                }
            }
            return;
        }
        if(inferred instanceof Generic){
            for (Identifier id : this.identifiers()) {
                id.assign(env,Generic.from(id,null));
            }
            return;
        }
        GCPErrorReporter.report(this, GCPErrCode.OUTLINE_MISMATCH,this+" is not an tuple");
    }

    @Override
    public List<Identifier> identifiers() {
        List<Identifier> ids = new ArrayList<>();
        findIdentifiers(ids, begins);
        findIdentifiers(ids, ends);
        return ids;
    }

    private void findIdentifiers(List<Identifier> ids, List<Node> list) {
        for (Node id : list) {
            if (id instanceof UnpackNode) {
                ids.addAll(((UnpackNode) id).identifiers());
                continue;
            }
            if (!((id instanceof UnderLineNode) || (id instanceof TypeNode))) {
                ids.add(cast(id));
            }
        }
    }

    public List<Node> begins() {
        return this.begins;
    }

    public List<Node> ends() {
        return this.ends;
    }
}

