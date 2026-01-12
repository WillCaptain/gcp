package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class VariableDeclarator extends Statement {
    private final VariableKind kind;
    private List<Assignment> assignments = new ArrayList<>();

    public VariableDeclarator(AST ast, VariableKind kind) {
        super(ast);
        this.kind = kind;
    }

    public Assignment declare(Assignable name, TypeNode declared, Expression value){
        Assignment assignment = this.addNode(new Assignment(new Variable(name,this.kind.mutable(),declared), value));
        this.assignments.add(assignment);
        return assignment;
    }
    public Assignment declare(Assignable name, Expression value){
        return this.declare(name,null,value);
    }

    public List<Assignment> assignments() {
        return this.assignments;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.kind.name().toLowerCase() + " ");
        for (Assignment assignment : this.assignments) {
            sb.append(assignment.lexeme() + ", ");
        }
        return sb.substring(0, sb.length() - 2) + ";";
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
    public VariableKind kind() {
        return this.kind;
    }
}
