package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;

import java.util.ArrayList;
import java.util.List;

public class Selections extends Expression {
    private List<Arm> arms = new ArrayList<>();

    private Expression others = null;

    public Selections(AST ast) {
        super(ast, null);
    }

    public Selections addArm(Arm arm) {
        this.arms.add(arm);
        return this;
    }

    public void setOthers(Expression others) {
        this.others = others;
    }

    @Override
    public Outline outline() {
        this.outline = null;
        for (Arm arm : this.arms) {
            if (this.outline == null) {
                this.outline = arm.outline();
            } else {
                if (this.outline.equals(arm.outline())) continue;
                this.outline = Option.from(this,this.outline, arm.outline());
            }
        }
        return Option.from(this,this.outline, others == null ? ProductADT.Ignore : others.outline());
    }
}
