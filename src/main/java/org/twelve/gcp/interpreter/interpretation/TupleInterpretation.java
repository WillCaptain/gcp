package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.TupleNode;
import org.twelve.gcp.node.statement.MemberNode;

import java.util.*;

public class TupleInterpretation implements Interpretation<TupleNode> {
    @Override
    public Value interpret(TupleNode node, Interpreter interp) {
        Map<String, MemberNode> members = node.members();

        EntityValue symbolBase = null;
        String symbolTag = null;
        if (node.base() != null) {
            Value baseVal = interp.eval(node.base());
            if (baseVal instanceof EntityValue ev && ev.hasSymbol() && ev.ownFields().isEmpty()) {
                symbolTag = ev.symbolTag();
            } else if (baseVal instanceof EntityValue ev) {
                symbolBase = ev;
            }
        }

        List<Value> placeholders = new ArrayList<>(Collections.nCopies(members.size(), UnitValue.INSTANCE));
        TupleValue tuple = new TupleValue(placeholders);

        Environment tupleEnv = interp.env().child();
        tupleEnv.define("this", tuple);
        if (symbolBase != null) {
            tupleEnv.define("base", symbolBase);
            tupleEnv.define("baseNode", symbolBase);
        }

        Environment saved = interp.env();
        interp.setEnv(tupleEnv);
        List<Value> finalElements = new ArrayList<>();
        try {
            for (int i = 0; i < members.size(); i++) {
                MemberNode member = members.get(String.valueOf(i));
                finalElements.add(member != null ? interp.eval(member.expression()) : UnitValue.INSTANCE);
            }
        } finally {
            interp.setEnv(saved);
        }

        if (symbolTag != null) {
            LinkedHashMap<String, Value> tupleFields = new LinkedHashMap<>();
            for (int i = 0; i < finalElements.size(); i++)
                tupleFields.put(String.valueOf(i), finalElements.get(i));
            return EntityValue.sharedTagged(tupleFields, null, symbolTag);
        }
        return new TupleValue(finalElements);
    }
}
