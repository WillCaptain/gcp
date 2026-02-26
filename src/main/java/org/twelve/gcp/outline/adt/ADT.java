package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.Literal;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.twelve.gcp.common.Tool.cast;

public abstract class ADT implements Outline {
    private final AST ast;
    protected long id;

    protected final Map<String, EntityMember> members = new HashMap<>();
    private int scopeLayer;

    public ADT(AST ast) {
        this.ast = ast;
        this.id = ast.Counter.getAndIncrement();
//        this.init();
    }

    @Override
    public AST ast() {
        return this.ast;
    }

    public boolean loadBuiltInMethods() {
        if (this.members.containsKey(CONSTANTS.TO_STR)) return false;
        EntityMember toString = EntityMember.from(CONSTANTS.TO_STR, FirstOrderFunction.from(this.ast(), this.ast().String, this.ast().Unit),
                Modifier.PUBLIC, false, null, true);
        this.members.put(CONSTANTS.TO_STR, toString);
        return true;
    }


    @Override
    public long id() {
        return this.id;
    }

    public List<EntityMember> members() {
        return members.values().stream().toList();
    }

    public Optional<EntityMember> getMember(String name) {
        return this.members().stream().filter(m -> m.name().equals(name)).findFirst();
    }

    @Override
    public boolean containsUnknown() {
        return this.members().stream().anyMatch(m -> m.outline.containsUnknown());
    }

    @Override
    public boolean containsIgnore() {
        return this.members().stream().anyMatch(m -> m.outline.containsIgnore());
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (!(another instanceof ProductADT)) return false;//若对方不是product adt，交由对方的tryYouAreMe去判定
        if (another instanceof Literal) return false;

        //another的每一个成员，this都应有一个member对应，方可满足is关系
        // 例外：Literal类型字段无需在构造时提供，其值由字面量自动注入
        ProductADT extended = cast(another);
        for (EntityMember member : extended.members()) {
            if (member.isDefault()) continue;
            Optional<EntityMember> found = this.members().stream().filter(m -> m.name().equals(member.name())).findFirst();
            if (!found.isPresent()) {
                if (member.outline() instanceof Literal) continue;
                return false;
            }
            if (!found.get().outline().is(member.outline())) {
                return false;
            }
        }
        return true;
    }
}
