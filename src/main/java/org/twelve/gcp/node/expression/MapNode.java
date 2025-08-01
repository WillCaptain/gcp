package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;

import java.util.HashMap;
import java.util.Map;

public class MapNode  extends Expression {
    protected final Map<Node,Node> values = new HashMap<>();
    public MapNode(AST ast, TupleNode... tuples) {
        super(ast, null);
        for (TupleNode tuple : tuples) {
            values.put(tuple.get(0),tuple.get(1));
        }
    }
}
