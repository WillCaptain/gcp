package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class EntityTypeNode  extends TypeNode {
    protected final List<Variable> members = new ArrayList<>();
    public EntityTypeNode(List<Variable> members){
        this(members.getFirst().ast());
        for (Variable member : members) {
            this.members.add(this.addNode(member));
        }
    }
    public EntityTypeNode(AST ast){
        super(ast);
    }

    public List<Variable> members(){
        return this.members;
    }

    @Override
    public String lexeme() {
       if(members.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (Variable member : members) {
            if(member.mutable()) sb.append("var ");
            sb.append(member.lexeme()+", ");
        }
        return sb.substring(0,sb.length()-2)+"}";
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
