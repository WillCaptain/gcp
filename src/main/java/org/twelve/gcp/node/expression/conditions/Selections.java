package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.common.SELECTION_TYPE;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

/**
 * unify if, match, ternary to same structure with
 * selections contains multi arms
 * each arm has its own predicate and consequence
 * @author huizi 2025
 */
public class Selections extends Expression {
    private final List<Arm> arms = new ArrayList<>();
    private final SELECTION_TYPE selectionType;

    public Selections(AST ast, Location loc, SELECTION_TYPE selectionType,Arm... arms) {
        super(ast, loc);
        this.selectionType = selectionType;
        for (Arm arm : arms) {
            this.arms.add(arm);
            this.addNode(arm);
        }
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public SELECTION_TYPE selectionType(){
        return this.selectionType;
    }

    public List<Arm> arms(){
        return new ArrayList<>(this.arms);
    }

    @Override
    public String lexeme() {
        return this.selectionType.lexeme(this);
    }
}
