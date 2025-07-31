package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.Variable;

import java.util.ArrayList;
import java.util.List;

public class TupleTypeNode extends EntityTypeNode {
    private static List<Variable> createMembers(List<TypeNode> members) {
        List<Variable> vs = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            TypeNode m = members.get(i);
            vs.add(new Variable(new Identifier(m.ast(), new Token<>(String.valueOf(i))), false, m));
        }
        return vs;
    }

    public TupleTypeNode(List<TypeNode> members) {
        super(createMembers(members));
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder("(");
        for (Variable member : members) {
            sb.append(member.declared().lexeme()).append(", ");
        }
        return sb.substring(0,sb.length()-2)+")";
    }
}
