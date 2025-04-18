package org.twelve.gcp.common;

import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.Selections;

public enum SELECTION_TYPE {
    IF {
        @Override
        public String lexeme(Selections selections) {
            StringBuilder sb = new StringBuilder();
            for (Arm arm : selections.arms()) {
                if (arm == selections.arms().getLast()) {
                    sb.append(arm.consequence().lexeme());
                } else {
                    sb.append("if(")
                            .append(arm.test().lexeme())
                            .append(")")
                            .append(arm.consequence().lexeme())
                            .append(" else ");
                }
            }
            return sb.toString();
        }
    }, MATCH {
        @Override
        public String lexeme(Selections selections) {
            return "";
        }
    }, TERNARY {
        @Override
        public String lexeme(Selections selections) {
            StringBuilder sb = new StringBuilder();
            Arm first = selections.arms().getFirst();
            Arm last = selections.arms().getLast();
            sb.append(first.test().lexeme())
                    .append("?")
                    .append(first.consequence().lexeme(), 3, first.consequence().lexeme().length() - 2)
                    .append(":")
                    .append(last.consequence().lexeme(), 3, last.consequence().lexeme().length() - 2);
            return sb.toString();
        }
    };

    public abstract String lexeme(Selections selections);
}
