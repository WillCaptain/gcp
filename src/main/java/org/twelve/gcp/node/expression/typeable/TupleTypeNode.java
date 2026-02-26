package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class TupleTypeNode extends EntityTypeNode {
    private static List<Variable> createMembers(List<TypeNode> members) {
        List<Variable> vs = new ArrayList<>();
        boolean isBegin = true;
        for (int i = 0; i < members.size(); i++) {
            TypeNode m = members.get(i);
            if (isBegin) {
                if (m instanceof OtherTypeNode) isBegin = false;
            } else {
                if (m instanceof OtherTypeNode) {
                    GCPErrorReporter.report(m, GCPErrCode.INVALID_SYMBOL);
                    continue;
                }
            }
            int index = isBegin ? i : (members.size() - i);
            vs.add(new Variable(new Identifier(m.ast(), new Token<>(String.valueOf(index))), false, m));
        }
        return vs;
    }

    public TupleTypeNode(List<ReferenceNode> refs, List<TypeNode> members) {
        super(refs,createMembers(members));
    }

    public TupleTypeNode(AST ast) {
        super(ast);
    }

    @Override
    public String lexeme() {
        if (this.members.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("(");
        for (Variable member : members) {
            sb.append(member.declared().lexeme()).append(", ");
        }
        return sb.substring(0, sb.length() - 2) + ")";
    }
    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
}
