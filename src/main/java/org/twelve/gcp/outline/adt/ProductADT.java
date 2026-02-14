package org.twelve.gcp.outline.adt;

import com.sun.xml.ws.developer.Serialization;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.builtin.BuildInOutline;

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
               if(v.isDefault()) return;
                finalCopied.members.put(k,EntityMember.from(v.name(), v.outline.copy(cache), v.modifier(), v.mutable()==Mutable.True, v.node(),v.isDefault()));
            });
            cache.put(this,copied);
        }
        return copied;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        List<EntityMember> ms = this.members().stream().filter(m->!m.isDefault()).toList();
        for (int i = 0; i < ms.size(); i++) {
            sb.append(ms.get(i).toString() + (i == ms.size() - 1 ? "" : ","));
        }
        sb.append("}");
        return sb.toString();
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
        EntityMember m = this.members.get(member.name());
        if (m == null || !m.outline().inferred()) {//没有重载,或者第n次infer，覆盖前一次infer结果
            this.members.put(member.name(), member);
            return true;
        }

        if (m.outline().equals(member.outline())) return true;


        if (m.outline() instanceof Poly) {//第n次重载
            return ((Poly) m.outline()).sum(member.outline(), member.mutable().toBool());
        } else {//第一次重载
            if (m.node() == member.node()) {
                this.members.put(member.name(), member);
                return true;
            }
            Poly overwrite = Poly.from(m.node());
            overwrite.sum(m.outline(), m.mutable().toBool());
//            overwrite.sum(member.outline(), member.mutable().toBool());
            if (overwrite.sum(member.outline(), member.mutable().toBool())) {
                this.members.put(member.name(), EntityMember.from(m.name(), overwrite, m.modifier()));
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean replaceMember(String name, Outline outline) {
        EntityMember removed = this.members.remove(name);
        return this.addMember(name, outline, removed.modifier(), removed.mutable() == Mutable.True, removed.node());
    }

    public boolean addMember(String name, Outline outline, Modifier modifier, Boolean mutable, Identifier node) {
        return this.addMember(EntityMember.from(name, outline, modifier, mutable, node,false));
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
                if (!found.get().outline().equals(member.outline())) {
                    members.remove(found.get());
//                    Poly overwrite = Poly.from(found.get().node());
                    Poly overwrite = Poly.create(this.ast());

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

    public BuildInOutline buildIn() {
        return this.buildIn;
    }
}