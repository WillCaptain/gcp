package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.SimpleLocation;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.Outline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

/**
 * entity是有成员变量的object
 * 但是基础类型都可以有成员变量，都可表现为Product ADT
 * entity建立有两种场景：
 * 1. 纯object: {name:String, gender:Male|Female}
 * 2. 基础类型扩展出来的object： String{size:Integer}
 */
public class EntityNode extends ValueNode<EntityNode>{
    //    private final Map<String, MemberNode> members = new HashMap<>();
    protected Node base;
    protected final Long scope;

    public EntityNode(List<MemberNode> members, Node base, Location loc) {
        super(members.getFirst().ast(), loc);
        this.scope = this.ast().scopeIndexer().incrementAndGet();
        if(base!=null) {
            this.addNode(base);
        }
        for (MemberNode member : members) {
            this.addNode(member);
        }
        this.base = base;
    }

    public EntityNode(List<MemberNode> members, Node base) {
        this(members, base,null);
    }

    public EntityNode(List<MemberNode> members) {
        this(members,null, null);
    }

    @Override
    public Location loc() {
        if (super.loc() != null) return super.loc();

        Long min = this.nodes().stream().map(m -> m.loc().start()).min((m1, m2) -> m1 < m2 ? -1 : 1).get();
        Long max = this.nodes().stream().map(m -> m.loc().start()).min((m1, m2) -> m1 > m2 ? -1 : 1).get();
        return new SimpleLocation(min, max);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder((base == null ? "" : base.lexeme()) + "{\n");
        int index = 0;
        for (Node node : this.nodes()) {
            if(node==this.base) continue;
            String[] lines = node.lexeme().split("\n");

            for (int i = 0; i < lines.length; i++) {
                sb.append("  ");
                sb.append(lines[i]);
                if (i == lines.length - 1) break;
                sb.append("\n");
            }
            sb.append(",\n");
        }
        sb.replace(sb.length()-2,sb.length(),"\n}");
//        sb.append("}");
        return sb.toString();
    }

    @Override
    public Long scope() {
        return this.scope;
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public Node base() {
        return this.base;
    }

    public Map<String, MemberNode> members() {
        Map<String, MemberNode> ms = new HashMap<>();
        for (Node node : this.nodes()) {
            if(node==this.base) continue;
            MemberNode m = cast(node);
            ms.put(m.identifier().name(), m);
        }
        return ms;
    }

    @Override
    public boolean isSame(EntityNode entity) {
        return false;
    }

    public boolean inferred() {
        return this.outline.inferred();
    }
}
