package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.expression.typeable.WrapperTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.decorators.This;
import org.twelve.gcp.outline.projectable.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

/**
 * Entity是实例化后的Product ADT的通用类型
 * Entity可以有不同的基础类型（buildIn）
 * 基础类型为Any的时候，Entity可等价于Object
 * Primitive类型的扩展后类型皆为Entity
 *
 * @author huizi 2024
 */
public class Entity extends ProductADT implements Projectable, ReferAble {
    private final List<Reference> references;
    //    private final List<Reference> references;
    private long id;
    /**
     * Entity的基Entity
     * entity2 = entity1{name="Will“}
     * 此时entity1就是entity2的基entity
     */
    protected Outline base;
    /**
     * Entity不是标准类型，所以一定会对应一个节点
     */
    protected final Node node;

    protected Entity(Node node, AST ast, Outline base, List<EntityMember> extended, List<Reference> references) {
        super(ast, ast.Any.buildIn(), extended);
        this.node = node;
        this.id = ast.Counter.getAndIncrement();
        this.base = base;
        this.references = references;
    }

    protected Entity(Node node, AST ast, Outline base, List<EntityMember> extended) {
        this(node, ast, base, extended, new ArrayList<>());
    }

    public Entity produce(Entity another) {
        return Entity.from(this.node(), this.interact(this.members(), another.members()));
    }

    public Poly produce(Poly another) {
        return cast(another.sum(this, false));
    }

    public static Entity from(Node node, Outline base, List<EntityMember> extended) {
        return from(node, base, extended, new ArrayList<>());
    }

    public static Entity from(Node node, Outline base, List<EntityMember> extended, List<Reference> references) {
        return new Entity(node, node.ast(), base, extended, references);
    }

    public static Entity fromRefs(Node node, List<Reference> references) {
        return new Entity(node, node.ast(), node.ast().Any, new ArrayList<>(), references);
    }

    public static Entity from(Node node) {
        return new Entity(node, node.ast(), node.ast().Any, new ArrayList<>());
    }

    public static Entity from(Node node, List<EntityMember> members) {
        return new Entity(node, node.ast(), node.ast().Any, members);
    }


//    public static Entity from(AST ast, List<EntityMember> members) {
//        return new Entity(null, ast, ast.Any, members);
//    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public List<Reference> references() {
        return this.references;
    }

    @Override
    public Outline project(List<OutlineWrapper> types) {
        Entity e = this;
        List<Reference> refs = new ArrayList<>(this.references);
        if(this.base instanceof ReferAble){
            refs.addAll(((ReferAble) this.base).references());
        }
        if (refs.size() < types.size()) {
            GCPErrorReporter.report(this.node, GCPErrCode.REFERENCE_MIS_MATCH);
        }
        for (int i = 0; i < types.size(); i++) {
            Reference me = refs.get(i);
            OutlineWrapper you = types.get(i);
            if (you == null) break;
            if (you.outline().is(me)) {
                e = cast(e.project(me, you));
            } else {
                GCPErrorReporter.report(you.node(), GCPErrCode.REFERENCE_MIS_MATCH);
                e = cast(e.project(me, new OutlineWrapper(you.node(), me.guess())));
            }
        }

        return e.eventual();
    }

    @Override
    public Entity copy() {
        return new Entity(this.node, this.ast(), this.base, this.members());//TODO
    }

    @Override
    public Entity copy(Map<Outline, Outline> cache) {
        Entity copied = cast(cache.get(this));
        if (copied == null) {
            List<EntityMember> members = new ArrayList<>();
            for (EntityMember m : this.members()) {
                if (m.isDefault()) continue;
                members.add(EntityMember.from(m.name(), m.outline.copy(cache), m.modifier(), m.mutable() == Mutable.True, m.node(), m.isDefault()));
            }
            copied = new Entity(this.node, this.ast(), this.base, members);
            cache.put(this, copied);
        }
        return copied;
    }

    private List<EntityMember> baseMembers() {
        if (this.base == null || !(this.base instanceof ProductADT)) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(((ProductADT) this.base).members());
        }
    }

    public Outline base() {
        return this.base == null ? this.ast().Any : this.base;
    }

    @Override
    public List<EntityMember> members() {
        return this.interact(super.members(), this.baseMembers());
    }

    @Override
    public boolean inferred() {
        return this.members().stream().allMatch(m -> m.outline().inferred());
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline base = this.base;
        if (this.base != null && this.base instanceof Projectable) {
            base = ((Projectable) this.base).project(projected, projection, session);
        }
        if (this.id() == projected.id()) {//project myself:{a} project{a,b}
            Entity outline = Entity.from(this.node(), base, new ArrayList<>());
            for (EntityMember yourMember : ((ADT) projection).members()) {//match projection members
                Optional<EntityMember> myMember = this.getMember(yourMember.name());//find matched member in projected
                if (myMember.isPresent()) {//project a
                    if (myMember.get().outline() instanceof Projectable) {//project member if me is projectable
                        Projectable me = cast(myMember.get().outline());
                        outline.addMember(yourMember.name(), me.project(me, yourMember.outline(), session),
                                yourMember.modifier(), yourMember.mutable() == Mutable.True, yourMember.node());
                        continue;
                    }
                    //if you are genericable and i'm not, add me as your constraint
                    if (yourMember.outline() instanceof Genericable<?, ?>) {
                        ((Genericable<?, ?>) yourMember.outline()).addHasToBe(myMember.get().outline());
                    }
                }
                outline.addMember(yourMember.name(), yourMember.outline(), yourMember.modifier(), yourMember.mutable() == Mutable.True, yourMember.node());
            }
            if (outline.is(this)) {
//                session.addProjection(projected, outline);
                return outline;
            } else {
                GCPErrorReporter.report(projection.ast(), projection.node(), GCPErrCode.PROJECT_FAIL,
                        projection.node() + CONSTANTS.MISMATCH_STR + this);
                return this.guess();
            }
        } else {//project possible members
//            Entity outline = this.node()==null?Entity.from(this.ast()):Entity.from(this.node());
            Entity outline = Entity.from(this.node(), base, new ArrayList<>());
            for (EntityMember m : this.members()) {
                if (m.outline() instanceof Projectable) {
                    Projectable p = cast(m.outline());
                    outline.addMember(m.name(), p.project(projected, projection, session), m.modifier(),
                            m.mutable() == Mutable.True, m.node());
                } else {
                    outline.addMember(m.name(), m.outline(), m.modifier(), m.mutable() == Mutable.True, m.node());
                }
            }
//            session.addProjection(this, outline);
            return outline;
        }
    }

    @Override
    public Outline guess() {
        List<EntityMember> members = new ArrayList<>();
        for (EntityMember m : this.members()) {
            if (m.isDefault()) continue;
            Outline guessed = m.outline() instanceof Projectable ? ((Projectable) m.outline()).guess() : m.outline();
            members.add(EntityMember.from(m.name(), guessed, m.modifier(), m.mutable() == Mutable.True, m.node(), m.isDefault()));
        }
        return Entity.from(this.node(), members);
    }

    @Override
    public boolean emptyConstraint() {
        return this.members().stream().map(m -> m.outline).anyMatch(o -> o instanceof Projectable && ((Projectable) o).emptyConstraint());
    }

    @Override
    public boolean containsGeneric() {
        return this.members().stream().map(m -> m.outline).anyMatch(o -> o instanceof Projectable && ((Projectable) o).containsGeneric());
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
//        Entity projected;
        Outline base = null;
        if (this.base != null) {
            base = this.base.project(reference, projection);
        }
        List<EntityMember> ms = new ArrayList<>();
        for (String key : this.members.keySet()) {
            EntityMember m = this.members.get(key);
            Variable n = cast(m.node());
            Outline mProjected = m.outline().project(reference, projection);
            if (m.node() != null && n.declared() != null) {
                Outline declared = m.node().outline().project(reference, projection);
                if (declared.id() != m.node().outline().id()) {
                    n = new Variable(n.identifier(), n.mutable(), new WrapperTypeNode(n.ast(), declared));
                }
            }

            ms.add(EntityMember.from(m.name(), mProjected, m.modifier(), m.mutable().toBool(), n, m.isDefault()));
        }
        List<Reference> refs = new ArrayList<>(this.references);
        refs.remove(reference);
        return new Entity(this.node, this.ast(), base, ms, refs);
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another instanceof Entity) {
            return super.tryIamYou(another) && this.base().is(((Entity) another).base());
        } else {
            return super.tryIamYou(another);
        }
    }

    @Override
    public boolean equals(Outline another) {
        if (!(another instanceof Entity)) return false;
        Entity you = cast(another);
        if (this.members.size() != you.members.size()) return false;
        for (String k : this.members.keySet()) {
            if (you.members.get(k) == null) return false;
            if (!this.members.get(k).outline().equals(you.members.get(k).outline())) return false;
        }
        return true;
    }

    @Override
    public boolean containsReference() {
        return !this.references().isEmpty();
    }

    @Override
    public Outline melt(Outline outline) {
        if(this==outline) return this;
        Entity other = cast(outline);
        other.members.forEach((k, v) -> {
            Optional<EntityMember> m = this.getMember(k);
            if (m.isPresent()) {
                Outline melted = m.get().outline.melt(v.outline());
                this.replaceMember(k, melted);
            } else {
                this.addMember(v.name(), v.outline(), v.modifier(), v.mutable() == Mutable.True, v.node());
            }
        });
        return this;
    }

    @Override
    public void updateThis(ProductADT me) {
        for (EntityMember member : this.members()) {
//            member.outline().updateThis(this);
            member.outline().updateThis(me);
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!this.references.isEmpty()) {
            sb.append("<");
            sb.append(this.references.stream().map(r->r.toString().replaceAll("[<>]","")).collect(Collectors.joining(",")));
            sb.append(">");
        }
        sb.append(super.toString());
        return sb.toString();
    }
}
