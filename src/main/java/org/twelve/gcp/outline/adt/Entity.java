package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.WrapperTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
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
    private final Outline base;
    /**
     * Entity不是标准类型，所以一定会对应一个节点
     */
    protected final Node node;

    protected Entity(Node node, AST ast, Outline base, List<EntityMember> extended) {
        super(ast, ast.Any.buildIn(), extended);
        this.node = node;
        this.id = ast.Counter.getAndIncrement();
        this.base = base;
    }

    public Entity produce(Entity another) {
        return Entity.from(this.node(), this.interact(this.members(), another.members()));
    }

    public Poly produce(Poly another) {
        return cast(another.sum(this, false));
    }

    public static Entity from(Node node, ProductADT base, List<EntityMember> extended) {
        return from(node, base, extended, new ArrayList<>());
    }

    public static Entity from(Node node, ProductADT base, List<EntityMember> extended, List<Reference> references) {
        return new Entity(node, node.ast(), base, extended);
    }

    public static Entity from(Node node) {
        return new Entity(node, node.ast(), node.ast().Any, new ArrayList<>());
    }

    public static Entity from(Node node, List<EntityMember> members) {
        return new Entity(node, node.ast(), node.ast().Any, members);
    }

    /**
     * no node bind entity is for entity declare
     *
     * @param members
     * @return
     */
    public static Entity from(AST ast, List<EntityMember> members) {
        return new Entity(null, ast, ast.Any, members);
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public Entity copy() {
        return new Entity(this.node, this.ast(), this.base, this.members());
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
        return this.base;
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
        if (this.id() == projected.id()) {//project myself:{a} project{a,b}
            Entity outline = Entity.from(this.node());
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
                GCPErrorReporter.report(projection.ast(),projection.node(), GCPErrCode.PROJECT_FAIL,
                        projection.node() + CONSTANTS.MISMATCH_STR + this);
                return this.guess();
            }
        } else {//project possible members
//            Entity outline = this.node()==null?Entity.from(this.ast()):Entity.from(this.node());
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
            }

            ms.add(EntityMember.from(m.name(), mProjected, m.modifier(), m.mutable().toBool(), n, m.isDefault()));
        }

        return new Entity(this.node, this.ast(), null, ms);
    }
}
