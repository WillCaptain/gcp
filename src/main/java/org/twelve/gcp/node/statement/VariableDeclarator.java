package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class VariableDeclarator extends Statement {
    private final VariableKind kind;
    private List<Assignment> assignments = new ArrayList<>();

    public VariableDeclarator(OAST ast, VariableKind kind) {
        super(ast);
        this.kind = kind;
    }

    //let a:B;
    public Assignment declare(Token varToken, Outline declareOutline) {
        return this.declare(varToken, declareOutline, null);
    }

    //let a:B = c;
    public Assignment declare(Token varToken, Outline declareOutline, Expression value) {
        Identifier var = new Identifier(this.ast(), varToken, declareOutline,this.kind==VariableKind.VAR);
        Assignment assignment = this.addNode(new Assignment(var, value));
        this.assignments.add(assignment);
        return assignment;
    }

    public Assignment declare(Token varToken, Expression value) {
        return this.declare(varToken, ProductADT.Unknown, value);
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
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public VariableKind kind() {
        return this.kind;
    }
}
