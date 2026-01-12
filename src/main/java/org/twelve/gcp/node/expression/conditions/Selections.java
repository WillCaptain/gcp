package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
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
public abstract class Selections<A extends Arm> extends Expression {
    private final List<A> arms = new ArrayList<>();

    public Selections(A arm,A... arms) {
        super(arm.ast(), null);
        this.arms.add(this.addNode(arm));
        ;
        for (A a : arms) {
            this.arms.add(this.addNode(a));
        }
    }
    public Selections(AST ast) {
        super(ast, null);
    }

    public void addArm(A arm){
        this.arms.add(this.addNode(arm));
    }

    public List<A> arms(){
        return this.arms;
    }

    public boolean containsElse() {
        return this.arms.stream().anyMatch(Arm::isElse);
    }
    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
