package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.statement.MemberNode;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityInterpretation implements Interpretation<EntityNode> {
    @Override
    public Value interpret(EntityNode node, Interpreter interp) {
        EntityValue baseVal = null;
        if (node.base() != null) {
            Value b = interp.eval(node.base());
            if (b instanceof EntityValue ev) baseVal = ev;
        }

        String symbolTag = (baseVal != null && baseVal.hasSymbol() && baseVal.ownFields().isEmpty())
                ? baseVal.symbolTag() : null;

        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        EntityValue self = symbolTag != null
                ? EntityValue.sharedTagged(fields, null, symbolTag)
                : EntityValue.shared(fields, baseVal);

        Environment entityEnv = interp.env().child();
        entityEnv.define("this", self);
        if (baseVal != null) {
            entityEnv.define("base", baseVal);
            entityEnv.define("baseNode", baseVal);
        }

        Environment saved = interp.env();
        interp.setEnv(entityEnv);
        try {
            for (Map.Entry<String, MemberNode> entry : node.members().entrySet()) {
                Value v = interp.eval(entry.getValue().expression());
                fields.put(entry.getKey(), v);
                entityEnv.define(entry.getKey(), v);
            }
        } finally {
            interp.setEnv(saved);
        }
        return self;
    }
}
