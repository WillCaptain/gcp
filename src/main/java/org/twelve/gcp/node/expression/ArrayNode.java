package org.twelve.gcp.node.expression;

import com.sun.istack.NotNull;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

public class ArrayNode extends Expression {
    private final Expression[] values;
    /**
     * processor convert array data to another value
     */
    private Expression processor;
    private Expression step = null;
    private Expression end = null;
    private Expression begin = null;
    /**
     * processor only process condition satisfied data
     */
    private Expression condition = null;

    public ArrayNode(AST ast, Expression[] values) {
        super(ast, null);
        this.values = values;
        if (values != null) {
            for (Expression value : values) {
                if (value != null) this.addNode(value);
            }
        }
    }

    public ArrayNode(AST ast) {
        this(ast,new Expression[0]);
    }

    public ArrayNode(AST ast, Expression begin, @NotNull Expression end, Expression step,Expression processor, Expression condition) {
        super(ast, null);
        this.processor = processor;
        this.condition = condition;
        this.begin = begin;
        this.end = end;
        this.step = step;
        this.values = null;
    }

    @Override
    public String lexeme() {
        if (this.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        if (values == null) {
            if (this.begin != null) sb.append(this.begin.lexeme());
            sb.append("...").append(this.end.lexeme());
            if (this.step != null) {
                sb.append(",").append(this.step.lexeme());
            }
            if(this.processor!=null){
                sb.append(",").append(this.processor.lexeme());
            }
            if (this.condition != null) {
                sb.append(",").append(this.condition.lexeme());
            }
        } else {
            sb.append(Arrays.stream(values).map(AbstractNode::lexeme).collect(Collectors.joining(",")));
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean isEmpty() {
        return this.values != null && this.values.length == 0;
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public Expression[] values() {
        return this.values;
    }

    public Expression processor(){
        return this.processor;
    }
    public Expression step() {
        return this.step;
    }

    public Expression begin() {
        return this.begin;
    }

    public Expression end() {
        return this.end;
    }

    public Expression condition() {
        return this.condition;
    }
}
