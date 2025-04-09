package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPRuntimeException;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.twelve.gcp.common.Tool.cast;

/**
 * Entity是实例化后的Product ADT的通用类型
 * Entity可以有不同的基础类型（buildIn）
 * 基础类型为Any的时候，Entity可等价于Object
 * Primitive类型的扩展后类型皆为Entity
 *
 * @author huizi 2024
 */
public class Entity extends ProductADT implements Projectable {
    private long id;
    /**
     * Entity的基Entity
     * entity2 = entity1{name="Will“}
     * 此时entity1就是entity2的基entity
     */
    private final ProductADT base;
    /**
     * Entity不是标准类型，所以一定会对应一个节点
     */
    private final Node node;

    private Entity(Node node, ProductADT base, List<EntityMember> extended) {
        super(base == null ? Outline.Any : base.buildIn, extended);
        this.node = node;
        this.id = Counter.getAndIncrement();
        this.base = base;
    }

    private Entity(Node node, BuildInOutline buildIn, List<EntityMember> members) {
        super(buildIn, members);
        this.node = node;
        this.id = Counter.getAndIncrement();
        this.base = null;
    }


    /**
     * union two outlines extensions to one extension
     *
     * @return merged product adt
     */
    public static Entity produce(Node node, ProductADT you, ProductADT me) {
        Entity entity;
        if (you.maybe(me)) {
            entity = new Entity(node, me.buildIn, new ArrayList<>());
        } else if (me.maybe(you)) {
            entity = new Entity(node, you.buildIn, new ArrayList<>());
        } else {
            throw new GCPRuntimeException(GCPErrCode.OUTLINE_MISMATCH);
        }
        entity.addMembers(you.members());
        entity.addMembers(me.members());
        return entity;
    }

    public static Entity from(Node node, ProductADT base, List<EntityMember> extended) {
        return new Entity(node, base, extended);
    }

    public static Entity from(Node node) {
        return new Entity(node, Outline.Any, new ArrayList<>());
    }

    public static Entity from(Node node, BuildInOutline buildIn) {
        return new Entity(node, buildIn, new ArrayList<>());
    }

    /**
     * no node bind entity is for entity declare
     *
     * @param members
     * @return
     */
    public static Entity from(BuildInOutline buildIn, List<EntityMember> members) {
        return new Entity(null, buildIn, members);
    }

    /**
     * no node bind entity is for entity declare
     *
     * @param members
     * @return
     */
    public static Entity from(List<EntityMember> members) {
        return new Entity(null, Outline.Any, members);
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public Entity copy() {
        return new Entity(this.node, this.buildIn, this.members());
    }

    @Override
    public List<EntityMember> members() {
        List<EntityMember> members = super.members();
        List<EntityMember> base = this.baseMembers();
        for (EntityMember member : base) {
            Optional<EntityMember> found = members.stream().filter(m -> m.name().equals(member.name())).findFirst();
            if (found.isPresent()) {
                if (!found.get().outline().equals(member.outline())) {
                    members.remove(found.get());
                    Poly overwrite = Poly.create();

                    overwrite.sum(member.outline(), member.node(), member.mutable().toBool());
                    overwrite.sum(found.get().outline(), found.get().node(), found.get().mutable().toBool());
                    members.add(EntityMember.from(member.name(), overwrite, member.modifier()));
                }
            } else {
                members.add(member);
            }
        }
        return members;
    }

    private List<EntityMember> baseMembers() {
        if (this.base == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(this.base.members());
        }
    }

    public ProductADT base() {
        return this.base;
    }

    @Override
    public boolean inferred() {
        for (EntityMember member : this.members()) {
            if (!member.outline.inferred()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (this.id() == projected.id()) {//project myself
//            if (session.getProjection(projected) != null) {
//                return session.getProjection(projected);
//            }
            Entity outline = Entity.from(projection.node());
//            List<EntityMember> projectedMembers = this.members();//get projected members
            for (EntityMember m2 : ((Entity) projection).members()) {//match projection members
                Optional<EntityMember> m1 =this.getMember(m2.name());//find matched member in projected
                if (m1.isPresent() && m1.get().outline() instanceof Projectable) {
                    Projectable p = cast(m1.get().outline());
                    outline.addMember(m2.name(), p.project(p, m2.outline(), session)
                            , m2.modifier(), m2.mutable() == Mutable.True, m2.node());
                } else {
                    outline.addMember(m2.name(), m2.outline(), m2.modifier(), m2.mutable() == Mutable.True, m2.node());
                }
            }
            if (outline.is(this)) {
//                session.addProjection(projected, outline);
                return outline;
            } else {
                ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
                Outline guessed = this.guess();
//                session.addProjection(projected, guessed);
                return guessed;
            }
        } else {//project possible members
            Entity outline = Entity.from(this.node());
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
            Outline guessed = m.outline() instanceof Projectable ? ((Projectable) m.outline()).guess() : m.outline();
            members.add(EntityMember.from(m.name(), guessed, m.modifier(), m.mutable() == Mutable.True, m.node()));
        }
        return Entity.from(this.buildIn, members);
    }
}
