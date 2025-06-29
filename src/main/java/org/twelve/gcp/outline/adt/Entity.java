package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPRuntimeException;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.WrapperTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.*;

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
    //    private final List<Reference> references;
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
//        this.references = references;
    }

    private Entity(Node node, BuildInOutline buildIn, List<EntityMember> members) {
        super(buildIn, members);
        this.node = node;
        this.id = Counter.getAndIncrement();
        this.base = null;
//        this.references = references;
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
        return from(node, base, extended, new ArrayList<>());
    }

    public static Entity from(Node node, ProductADT base, List<EntityMember> extended, List<Reference> references) {
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

    public static Entity from(List<EntityMember> members) {
        return from(Outline.Any, members);
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
    public Entity copy(Map<Long, Outline> cache) {
        Entity copied = cast(cache.get(this.id()));
        if(copied==null){
            List<EntityMember> members = new ArrayList<>();
            for (EntityMember m : this.members()) {
                members.add(EntityMember.from(m.name(), m.outline.copy(cache), m.modifier(), m.mutable()==Mutable.True, m.node()));
            }
            copied = new Entity(this.node,this.buildIn,members);
            cache.put(this.id(),copied);
        }
        return copied;
    }

    @Override
    public List<EntityMember> members() {
        return this.interact(super.members(),this.baseMembers());
//        List<EntityMember> members = super.members();
//        List<EntityMember> base = this.baseMembers();
//        for (EntityMember member : base) {
//            Optional<EntityMember> found = members.stream().filter(m -> m.name().equals(member.name())).findFirst();
//            if (found.isPresent()) {
//                if (!found.get().outline().equals(member.outline())) {
//                    members.remove(found.get());
//                    Poly overwrite = Poly.create();
//
//                    overwrite.sum(member.outline(), member.mutable().toBool());
//                    overwrite.sum(found.get().outline(), found.get().mutable().toBool());
//                    members.add(EntityMember.from(member.name(), overwrite, member.modifier()));
//                }
//            } else {
//                members.add(member);
//            }
//        }
//        return members;
    }

    private List<EntityMember> interact(List<EntityMember> one, List<EntityMember> another){
        List<EntityMember> members = new ArrayList<>(one);
        for (EntityMember member : another) {
            Optional<EntityMember> found = members.stream().filter(m -> m.name().equals(member.name())).findFirst();
            if (found.isPresent()) {
                if (!found.get().outline().equals(member.outline())) {
                    members.remove(found.get());
                    Poly overwrite = Poly.create();

                    overwrite.sum(member.outline(), member.mutable().toBool());
                    overwrite.sum(found.get().outline(), found.get().mutable().toBool());
                    members.add(EntityMember.from(member.name(), overwrite, member.modifier()));
                }
            } else {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public Outline interact(Outline another) {
        if(another instanceof Entity){
            return Entity.from(this.interact(this.members(),((Entity) another).members()));
        }
        if(another instanceof Poly){
            Outline result = this;
            for (Outline option : ((Poly) another).options) {
                result = result.interact(option);
            }
            return  result;
        }
        return super.interact(another);
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
        return this.members().stream().allMatch(m -> m.outline().inferred());
//        for (EntityMember member : this.members()) {
//            if (!member.outline.inferred()) {
//                return false;
//            }
//        }
//        return true;
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (this.id() == projected.id()) {//project myself:{a} project{a,b}
            Entity outline = Entity.from(projection.node());
            for (EntityMember yourMember : ((Entity) projection).members()) {//match projection members
                Optional<EntityMember> myMember = this.getMember(yourMember.name());//find matched member in projected
                if (myMember.isPresent()) {//project a
                    if (myMember.get().outline() instanceof Projectable) {//project member if me is projectable
                        Projectable me = cast(myMember.get().outline());
                        outline.addMember(yourMember.name(), me.project(me, yourMember.outline(), session),
                                yourMember.modifier(), yourMember.mutable() == Mutable.True, yourMember.node());
                        continue;
                    }
                    //if you are genericable and i'm not, add me as your constraint
                    if(yourMember.outline() instanceof Genericable<?,?>){
                        ((Genericable<?, ?>) yourMember.outline()).addHasToBe(myMember.get().outline());
                    }
                }
                outline.addMember(yourMember.name(), yourMember.outline(), yourMember.modifier(), yourMember.mutable() == Mutable.True, yourMember.node());
            }
            if (outline.is(this)) {
//                session.addProjection(projected, outline);
                return outline;
            } else {
                ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL,
                        projection.node() + CONSTANTS.MISMATCH_STR + this);
                return this.guess();
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

    @Override
    public boolean emptyConstraint() {
        return this.members().stream().map(m->m.outline).anyMatch(o-> o instanceof Projectable && ((Projectable) o).emptyConstraint());
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        Entity projected;
        List<EntityMember> ms = new ArrayList<>();
        for (String key : this.members.keySet()) {
            EntityMember m = this.members.get(key);
            Variable n = m.node();
            Outline mProjected = m.outline().project(reference, projection);
            if (m.node() != null && m.node().declared() != null) {
                Outline declared = m.node().outline().project(reference, projection);
                if (declared.id() != m.node().outline().id()) {
                    n = new Variable(n.identifier(), n.mutable(), new WrapperTypeNode(n.ast(), declared));
                }
//                if(!mProjected.is(declared)){
//                    ErrorReporter.report(m.node(),GCPErrCode.OUTLINE_MISMATCH,mProjected+" doesn't match with "+declared);
//                }
            }

            ms.add(EntityMember.from(m.name(), mProjected, m.modifier(), m.mutable().toBool(), n));
        }
        if (this.base == null) {
            projected = new Entity(this.node, this.buildIn, ms);
        } else {
            projected = new Entity(this.node, (ProductADT) this.base.project(reference, projection), ms);
        }
        return projected;
    }
}
