package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DictNode extends Expression {
    private final Map<Expression,Expression> values = new LinkedHashMap<>();
    public DictNode(AST ast, Pair<Expression,Expression>... tuples) {
        super(ast, null);
        for (Pair<Expression,Expression> tuple : tuples) {
            values.put(tuple.key(),tuple.value());
        }
    }

    public Map<Expression,Expression> values(){
        return this.values;
    }

    @Override
    public String lexeme() {
        if (this.isEmpty()) return "[:]";
        StringBuilder sb = new StringBuilder("[");
        for (Node key : values.keySet()) {
            sb.append(key.lexeme()).append(":").append(values.get(key).lexeme()).append(", ");
        }
        return sb.substring(0,sb.length()-2)+"]";
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }
    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
