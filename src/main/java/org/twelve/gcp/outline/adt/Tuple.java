package org.twelve.gcp.outline.adt;

import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.common.TupleMatcher;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

import java.util.*;

import static org.twelve.gcp.common.Tool.cast;

public class Tuple extends Entity {


    public Tuple(Entity entity) {
        super(entity.node(), entity.ast(), entity.ast().Any, entity.members(),entity.references());
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another instanceof Tuple) {
            return this.iAmTuple(cast(another));
        }
        if (another instanceof Genericable<?, ?>) {
            return another.tryYouAreMe(this);
        }
        return false;
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (another instanceof Genericable<?, ?>) {
            return another.tryIamYou(this);
        }
        return false;
    }

    @Override
    public Outline guess() {
        return new Tuple(cast(super.guess()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        List<EntityMember> ms = this.members().stream().filter(m -> !m.isDefault()).toList();
        boolean isBegin = true;
        for (int i = 0; i < ms.size(); i++) {
            Integer now = Integer.valueOf(ms.get(i).name());
            if (now >= 0) {
                sb.append(ms.get(i).outline() + (i == ms.size() - 1 ? "" : ","));
//                sb.append(ms.get(i).toString().split(":")[1] + (i == ms.size() - 1 ? "" : ","));
            } else {
                if (isBegin) {
                    sb.append("...,");
                    isBegin = false;
                }
                sb.append(ms.get(i).outline() + (i == ms.size() - 1 ? "" : ","));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Tuple copy() {
        return new Tuple(super.copy());
    }

    @Override
    public Entity copy(Map<Outline, Outline> cache) {
        return new Tuple(super.copy(cache));
    }
    @Override
    public Outline project(List<OutlineWrapper> types) {
        return new Tuple(cast(super.project(types)));
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline base = this.base;
        if (this.base != null && this.base instanceof Projectable) {
            base = ((Projectable) this.base).project(projected, projection, session);
        }

        Entity ent = Entity.from(this.node(), base, new ArrayList<>());
        if (this.id() == projected.id()) {
            // projection may be a sum type (Option) when the Tuple is a variant of a union ADT.
            // Unwrap the first Tuple variant so TupleMatcher gets a concrete Tuple.
            Outline resolvedProjection = projection;
            if (projection instanceof SumADT sum) {
                resolvedProjection = sum.options().stream()
                        .filter(o -> o instanceof Tuple)
                        .findFirst()
                        .orElse(projection);
            }
            if (!(resolvedProjection instanceof Tuple)) {
                // Cannot match members to a non-Tuple projection; propagate as-is.
                for (EntityMember member : this.members()) {
                    ent.addMember(member.name(), member.outline(), member.modifier(),
                            member.mutable() == Mutable.True, member.node());
                }
                return new Tuple(ent);
            }
            TupleMatcher matcher = new TupleMatcher(cast(resolvedProjection));
            for (EntityMember member : this.members()) {
                Outline me = member.outline();
                Outline you = matcher.match(Integer.valueOf(member.name()));
                if (me instanceof Projectable) {
                    ent.addMember(member.name(), ((Projectable) me).project(cast(me), you, session), member.modifier(),
                            member.mutable() == Mutable.True, member.node());
                } else {
                    if (you instanceof Genericable<?, ?>) {
                        ((Genericable<?, ?>) you).addHasToBe(me);
                    }
                    ent.addMember(member.name(), you, member.modifier(), member.mutable() == Mutable.True, member.node());
                }
            }
//            return new Tuple(ent);
        } else {
            for (EntityMember member : this.members()) {
                Outline me = member.outline();
                if (me instanceof Projectable) {
                    ent.addMember(member.name(), ((Projectable) me).project(projected, projection, session), member.modifier(),
                            member.mutable() == Mutable.True, member.node());
                } else {
                    ent.addMember(member.name(), me, member.modifier(), member.mutable() == Mutable.True, member.node());
                }
            }
        }
        return new Tuple(ent);
    }

    public Outline get(Integer index) {
        return this.getMember(index.toString()).get().outline();
    }

    public Integer size() {
        return this.members().size();
    }

    public Map<Integer, Outline> structure() {
        Map<Integer, Outline> structure = new HashMap<>();
        this.members.forEach((k, v) -> {
            try {
                structure.put(Integer.valueOf(k), v.outline());
            } catch (Exception ex) {

            }
        });
        return structure;
    }

    protected boolean iAmTuple(Tuple you) {
        Map<Integer, Outline> yourStructure = you.structure();

        // 分别处理正索引和负索引
        List<Integer> myPositiveIndices = getPositiveIndicesSorted();
        List<Integer> myNegativeIndices = getNegativeIndicesSorted();
        List<Integer> yourPositiveIndices = you.getPositiveIndicesSorted();
        List<Integer> yourNegativeIndices = you.getNegativeIndicesSorted();

        // 1. 检查正索引约束
        // 如果对方有正索引，我的对应正索引必须匹配
        if (!yourPositiveIndices.isEmpty()) {
            for (int i = 0; i < yourPositiveIndices.size(); i++) {
                int yourIndex = yourPositiveIndices.get(i);
                Outline yourOutline = yourStructure.get(yourIndex);

                // 检查我是否有对应的正索引
                if (i < myPositiveIndices.size()) {
                    int myIndex = myPositiveIndices.get(i);
                    Outline myOutline = structure().get(myIndex);

                    if (!myOutline.is(yourOutline)) {
                        return false;
                    }
                } else {
                    // 如果我没有足够的正索引，检查是否可以由负索引覆盖
                    // 例如：t1=(String,String,Integer); t2=(String,String)
                    // t2有2个正索引，t1有3个正索引，前2个应该匹配
                    // 这里我们已经处理了前i个，所以这个情况应该已经在前面的循环中处理了
                    return false;
                }
            }
        }

        // 2. 检查负索引约束
        // 如果对方有负索引，我的对应负索引必须匹配
        if (!yourNegativeIndices.isEmpty()) {
            for (int i = 0; i < yourNegativeIndices.size(); i++) {
                int yourIndex = yourNegativeIndices.get(i);
                Outline yourOutline = yourStructure.get(yourIndex);

                // 检查我是否有对应的负索引
                if (i < myNegativeIndices.size()) {
                    int myIndex = myNegativeIndices.get(i);
                    Outline myOutline = structure().get(myIndex);

                    if (!myOutline.is(yourOutline)) {
                        return false;
                    }
                } else {
                    // 如果我没有对应的负索引，检查是否可以由正索引覆盖
                    // 例如：对方有-1，我可能有对应的最后一个正索引
                    int myPositiveCount = myPositiveIndices.size();
                    if (myPositiveCount > 0) {
                        // 对方-1对应我的最后一个正索引，对方-2对应我的倒数第二个正索引，以此类推
                        int positionFromEnd = yourNegativeIndices.size() - i;
                        if (positionFromEnd <= myPositiveCount) {
                            int myPosIndex = myPositiveCount - positionFromEnd;
                            int myIndex = myPositiveIndices.get(myPosIndex);
                            Outline myOutline = structure().get(myIndex);

                            if (!myOutline.is(yourOutline)) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }

        // 3. 特殊情况：混合约束检查
        // 例如：t1=(...,String,Integer); t2=(...,Integer)
        // t2只有-1:Integer，t1有-2:String和-1:Integer
        // 需要检查t1的-1是否匹配t2的-1
        if (!yourNegativeIndices.isEmpty() && myNegativeIndices.size() >= yourNegativeIndices.size()) {
            // 从最后开始匹配负索引
            for (int i = 0; i < yourNegativeIndices.size(); i++) {
                int otherIdx = yourNegativeIndices.get(i);
                int myIdx = myNegativeIndices.get(i);

                Outline otherType = yourStructure.get(otherIdx);
                Outline myType = structure().get(myIdx);

                if (!myType.is(otherType)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 获取正索引（>=0）并按升序排序
     */
    private List<Integer> getPositiveIndicesSorted() {
        List<Integer> positive = new ArrayList<>();
        for (Integer index : structure().keySet()) {
            if (index >= 0) {
                positive.add(index);
            }
        }
        Collections.sort(positive);
        return positive;
    }

    /**
     * 获取负索引（<0）并按从大到小排序（-1, -2, -3...）
     */
    private List<Integer> getNegativeIndicesSorted() {
        List<Integer> negative = new ArrayList<>();
        for (Integer index : structure().keySet()) {
            if (index < 0) {
                negative.add(index);
            }
        }
        // 负索引从大到小排序：-1 > -2 > -3
        Collections.sort(negative, (a, b) -> Integer.compare(b, a));
        return negative;
    }
}
