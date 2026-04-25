package org.twelve.gcp.outline.adt;

import com.sun.xml.ws.developer.Serialization;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.FieldMergeMode;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.decorators.Lazy;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.*;

import static org.twelve.gcp.common.Tool.cast;

/**
 * Product ADT
 * 所有基础类型都可以扩展为Product ADT，比如100的基础类型是Number_,但100.to_string()=="100"
 * 所以Number是Product ADT，其基础类型为Number_
 *
 * @author huizi 2025
 */
@Serialization
public abstract class ProductADT extends ADT {

    /**
     * Product ADT对应的基础类型
     */
    protected BuildInOutline buildIn;

    protected ProductADT(AST ast,BuildInOutline buildIn) {
        super(ast);
//        this.node = node;
        this.buildIn = buildIn;
    }

    protected ProductADT(AST ast, BuildInOutline buildIn, List<EntityMember> members) {
        this(ast,buildIn);
        this.addMembers(members);
    }

    /**
     * 如果都是ProductADT，先比较基本类型的outline是否一致，以判断Product is关系是否成立
     *
     * @param another
     * @return
     */
    @Override
    public boolean maybe(Outline another) {
        if (another instanceof ProductADT) {
            return this.buildIn.is(((ProductADT) another).buildIn);
        }
        return true;
    }

    @Override
    public boolean is(Outline another) {
        return this.maybe(another) && super.is(another);
    }

    @Override
    public boolean canBe(Outline another) {
        return this.maybe(another) && super.canBe(another);
    }

    /**
     * 浅拷贝
     *
     * @return 新的Product ADT
     */
    @Override
    public ProductADT copy() {
        try {
            ProductADT copied = cast(super.copy());
            copied.members.putAll(this.members);
            return copied;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ProductADT copy(Map<Outline, Outline> cache) {
        ProductADT copied = cast(cache.get(this));
        if(copied==null){
            copied = cast(super.copy());
            ProductADT finalCopied = copied;
            this.members.forEach((k, v)->{
               // Skip only built-in default methods (isDefault && !hasDefaultValue) —
               // things like `to_str`, `abs` that are re-attached by loadBuiltInMethods()
               // on the copy. User-declared default-value fields (isDefault && hasDefaultValue),
               // e.g. `id:0` or `age:18`, are real members and MUST be preserved — otherwise
               // generic instantiations of `{params}->Entity` (e.g. `create: {..}->Entity`)
               // drop them silently and downstream `.id` completion/inference fails.
               if(v.isDefault() && !v.hasDefaultValue()) return;
               EntityMember m = v.hasDefaultValue()
                   ? EntityMember.fromWithDefault(v.name(), v.outline.copy(cache), v.modifier(),
                                                   v.mutable()==Mutable.True, v.node(), v.defaultValueNode(), v.mergeMode())
                   : EntityMember.from(v.name(), v.outline.copy(cache), v.modifier(),
                                       v.mutable()==Mutable.True, v.node(), v.isDefault(), v.mergeMode());
               finalCopied.members.put(k, m);
            });
            cache.put(this,copied);
        }
        return copied;
    }

    @Override
    public String toString() {
        return this.guardedToString("{...}", () -> {
            StringBuilder sb = new StringBuilder("{");
            // Show all members except pure literal-type constants (isDefault=true, no default-value node).
            // Default-value fields (age: 100) are included since they represent real, user-facing members.
            List<EntityMember> ms = this.members().stream()
                    .filter(m -> !m.isDefault() || m.hasDefaultValue())
                    .toList();
            for (int i = 0; i < ms.size(); i++) {
                sb.append(ms.get(i).toString()).append(i == ms.size() - 1 ? "" : ",");
            }
            sb.append("}");
            return sb.toString();
        });
    }

//    public List<EntityMember> members() {
//        return members.values().stream().toList();
//    }

    /**
     * 添加member列表
     *
     * @param members 成员列表
     */
    public void addMembers(List<EntityMember> members) {
        for (EntityMember member : members) {
            if (!this.addMember(member) && this.node() != null) {
                GCPErrorReporter.report(this.node(), GCPErrCode.DUPLICATED_DEFINITION);
            }
        }
    }

    /**
     * 添加单个member
     *
     * @param member 成员信息
     * @return
     */
    private boolean addMember(EntityMember member) {
//        member.outline().updateHost(this);
        //查询是否有同名成员
        member.outline().updateThis(this);
//        if (member.outline() instanceof FirstOrderFunction && ((FirstOrderFunction) member.outline()).getThis() != null) {
//            ((FirstOrderFunction) member.outline()).getThis().setOrigin(this);
//        }
        EntityMember m = this.members.get(member.name());
        if (m == null || !m.outline().inferred()) {//没有重载,或者第n次infer，覆盖前一次infer结果
            this.members.put(member.name(), member);
            this.invalidateMembersCache();
            return true;
        }

        if (m.outline().equals(member.outline())) return true;
        if (m.node() == member.node()) {
            this.members.put(member.name(), member);
            this.invalidateMembersCache();
            return true;
        }

        EntityMember merged = mergeMember(m, member, this.node());
        if (merged == null) return false;
        this.members.put(member.name(), merged);
        this.invalidateMembersCache();
        return true;

    }

    public boolean replaceMember(String name, Outline outline) {
        EntityMember removed = this.members.remove(name);
        this.invalidateMembersCache();
        return this.addMember(name, outline, removed.modifier(), removed.mutable() == Mutable.True, removed.node());
    }

    public boolean addMember(String name, Outline outline, Modifier modifier, Boolean mutable, Identifier node) {
        return this.addMember(EntityMember.from(name, outline, modifier, mutable, node, false));
    }

    public boolean addMember(String name, Outline outline, Modifier modifier, Boolean mutable, Identifier node, FieldMergeMode mergeMode) {
        return this.addMember(EntityMember.from(name, outline, modifier, mutable, node, false, mergeMode));
    }

    public boolean addMember(String name, Outline outline, Modifier modifier, Boolean mutable, Identifier node, boolean isDefault) {
        return this.addMember(EntityMember.from(name, outline, modifier, mutable, node, isDefault));
    }

    public boolean addMember(String name, Outline outline, Modifier modifier, Boolean mutable, Identifier node,
                             boolean isDefault, FieldMergeMode mergeMode) {
        return this.addMember(EntityMember.from(name, outline, modifier, mutable, node, isDefault, mergeMode));
    }

    public boolean addMemberWithDefault(String name, Outline outline, Modifier modifier, Boolean mutable, Identifier node, org.twelve.gcp.ast.Node defaultValueNode) {
        return this.addMember(EntityMember.fromWithDefault(name, outline, modifier, mutable, node, defaultValueNode));
    }

    public boolean addMemberWithDefault(String name, Outline outline, Modifier modifier, Boolean mutable, Identifier node,
                                        org.twelve.gcp.ast.Node defaultValueNode, FieldMergeMode mergeMode) {
        return this.addMember(EntityMember.fromWithDefault(name, outline, modifier, mutable, node, defaultValueNode, mergeMode));
    }

    public boolean checkMember(String name, Outline outline) {
        EntityMember member = this.members.get(name);
        if (member == null) return false;
        return outline.is(member.outline());
    }



    protected List<EntityMember> interact(List<EntityMember> one, List<EntityMember> another){
        List<EntityMember> members = new ArrayList<>(one);
        for (EntityMember member : another) {
            Optional<EntityMember> found = members.stream().filter(m -> m.name().equals(member.name())).findFirst();
            if (found.isPresent()) {
                // Literal-type (isDefault=true) own members take strict precedence — no Poly union.
                // This preserves immutable field semantics for outline definitions such as
                // outline Man = Human{ gender: #Male } where gender must stay Literal(Male),
                // not become Poly(Gender, Literal(Male)).
                if (found.get().isDefault()) continue;
                if (!found.get().outline().equals(member.outline())) {
                    members.remove(found.get());
                    EntityMember merged = mergeMember(member, found.get(), this.node());
                    if (merged != null) {
                        members.add(merged);
                    }
                }
            } else {
                members.add(member);
            }
        }
        return members;
    }

    public static EntityMember mergeMember(EntityMember oldMember, EntityMember newMember, Node errorNode) {
        if (newMember.outline().equals(oldMember.outline())) return newMember;
        if (newMember.outline() instanceof Lazy || oldMember.outline() instanceof Lazy
                || newMember.outline().toString().contains("Lazy{")
                || oldMember.outline().toString().contains("Lazy{")
                || newMember.outline().containsUnknown() || oldMember.outline().containsUnknown()) {
            return newMember;
        }
        boolean newIsOld = newMember.outline().is(oldMember.outline());
        boolean oldIsNew = oldMember.outline().is(newMember.outline());

        return switch (newMember.mergeMode()) {
            case DEFAULT -> {
                if (newIsOld) yield newMember;
                reportMergeError(oldMember, newMember, errorNode);
                yield null;
            }
            case OVERRIDE -> {
                if (newIsOld || oldIsNew) yield newMember;
                reportMergeError(oldMember, newMember, errorNode);
                yield null;
            }
            case OVERLOAD -> {
                if (newIsOld) yield oldMember;
                if (oldIsNew) yield newMember;
                Poly overwrite = Poly.create(oldMember.outline().ast());
                overwrite.sum(oldMember.outline(), oldMember.mutable().toBool());
                overwrite.sum(newMember.outline(), newMember.mutable().toBool());
                yield EntityMember.from(oldMember.name(), overwrite, oldMember.modifier());
            }
        };
    }

    private static void reportMergeError(EntityMember oldMember, EntityMember newMember, Node fallback) {
        Node node = newMember.node() != null ? newMember.node() : fallback;
        if (node != null) {
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH,
                    "field '" + newMember.name() + "' type " + newMember.outline()
                            + " is not compatible with inherited " + oldMember.outline());
        }
    }

    public BuildInOutline buildIn() {
        return this.buildIn;
    }
}