package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;

import java.util.LinkedHashMap;
import java.util.Map;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

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
        for (AbstractNode key : values.keySet()) {
            sb.append(key.lexeme()).append(":").append(values.get(key).lexeme()).append(", ");
        }
        return sb.substring(0,sb.length()-2)+"]";
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }
    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }

}
