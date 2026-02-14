package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EntityTypeNode  extends TypeNode {
    protected final List<Variable> members = new ArrayList<>();
    private final List<ReferenceNode> refs = new ArrayList<>();
    private final long scope;

    public EntityTypeNode(List<Variable> members){
        this(new ArrayList<>(),members);
    }
    public EntityTypeNode(AST ast){
        super(ast);
        this.scope = ast.scopeIndexer().incrementAndGet();
    }

    public EntityTypeNode(List<ReferenceNode> refs, List<Variable> members) {
        this(members.getFirst().ast());
        for (Variable member : members) {
            this.members.add(this.addNode(member));
        }
        for (ReferenceNode ref : refs) {
            this.refs.add(this.addNode(ref));
        }
    }

    public List<Variable> members(){
        return this.members;
    }
    public List<ReferenceNode> refs(){
        return this.refs;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
       if(members.isEmpty()) return sb.append("{}").toString();
       if(!this.refs.isEmpty()){
           sb.append("<");
           sb.append(this.refs.stream().map(ReferenceNode::lexeme).collect(Collectors.joining(",")));
           sb.append(">");
       }
        sb.append("{");
        sb.append(members.stream().map(m->(m.mutable()?"var ":"")+m.lexeme()).collect(Collectors.joining(",")));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public Long scope() {
        return this.scope;
    }
}
