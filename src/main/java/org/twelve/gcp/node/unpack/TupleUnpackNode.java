package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.TupleNode;
import org.twelve.gcp.node.expression.UnderLineNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Tuple;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class TupleUnpackNode extends UnpackNode {
    private final List<Node> begins;
    private final List<Node> ends;

    public TupleUnpackNode(AST ast, List<Node> begins, List<Node> ends) {
        super(ast, null);
        this.begins = begins;
        this.ends = ends;
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
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if (!(inferred instanceof Tuple)) {
            GCPErrorReporter.report(ast(), this, GCPErrCode.OUTLINE_MISMATCH, inferred + " is not unpackable");
            return;
        }
        Tuple tuple = cast(inferred);
        Integer counter = assignBegins(begins, tuple, env);
        assignEnds(counter, ends, tuple, env);
    }

    private Integer assignBegins(List<Node> begins, Tuple tuple, LocalSymbolEnvironment env) {
        if (begins.isEmpty()) return 0;
        for (int i = 0; i < this.begins.size(); i++) {
            if (tuple.size() > i ) {
                if (this.begins.get(i) instanceof Assignable) {
                    ((Assignable) this.begins.get(i)).assign(env, tuple.get(i));
                }
            } else {
                GCPErrorReporter.report(this.begins.get(i), GCPErrCode.UNPACK_INDEX_OVER_FLOW);
                return i;
            }
        }
        return this.begins.size();
    }

    private void assignEnds(Integer border, List<Node> ends, Tuple tuple, LocalSymbolEnvironment env) {
        if (this.ends.isEmpty()) return;
        List<Node> reversed = ends.reversed();
        for (int i = 0; i < reversed.size(); i++) {
            if (border + i < tuple.size() ) {
                if (reversed.get(i) instanceof Assignable) {
                    ((Assignable) reversed.get(i)).assign(env, tuple.get(tuple.size() - 1 - i));
                }
            } else {
                GCPErrorReporter.report(reversed.get(i), GCPErrCode.UNPACK_INDEX_OVER_FLOW);
                return;
            }
        }
    }
    @Override
    public List<Identifier> identifiers() {
        List<Identifier> ids = new ArrayList<>();
        findIdentifiers(ids,begins);
        findIdentifiers(ids,ends);
        return ids;
    }

    private void findIdentifiers(List<Identifier> ids,List<Node> list) {
        for (Node id : list) {
            if(id instanceof UnpackNode){
                ids.addAll(((UnpackNode) id).identifiers());
                continue;
            }
            if(!(id instanceof UnderLineNode)){
                ids.add(cast(id));
            }
        }
    }
}
