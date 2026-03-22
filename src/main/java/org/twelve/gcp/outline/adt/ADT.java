package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.Literal;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.twelve.gcp.common.Tool.cast;

public abstract class ADT implements Outline {
    private static final ThreadLocal<Set<Long>> CHECKING_UNKNOWN = ThreadLocal.withInitial(HashSet::new);
    private final AST ast;
    protected long id;

    protected final Map<String, EntityMember> members = new HashMap<>();
    /**
     * Cached snapshot of {@link #members} values, invalidated whenever the map is mutated.
     * Avoids creating a new {@code List} on every {@link #members()} call — which is hot
     * inside type-compatibility loops ({@link #tryIamYou}, {@link #updateThis}, etc.).
     */
    private List<EntityMember> cachedMembersList = null;
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
        this.cachedMembersList = null;
        return true;
    }

    /** Invalidates the cached members snapshot. Called by subclasses whenever {@link #members} is mutated. */
    protected final void invalidateMembersCache() {
        this.cachedMembersList = null;
    }


    @Override
    public long id() {
        return this.id;
    }

    public List<EntityMember> members() {
        if (cachedMembersList == null) {
            cachedMembersList = List.copyOf(members.values());
        }
        return cachedMembersList;
    }

    public Optional<EntityMember> getMember(String name) {
        return Optional.ofNullable(members.get(name));
    }

    @Override
    public boolean containsUnknown() {
        if (!CHECKING_UNKNOWN.get().add(this.id())) return false;
        try {
            return members.values().stream().anyMatch(m -> m.outline.containsUnknown());
        } finally {
            CHECKING_UNKNOWN.get().remove(this.id());
        }
    }

    @Override
    public boolean containsIgnore() {
        return members.values().stream().anyMatch(m -> m.outline.containsIgnore());
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
            EntityMember found = this.members.get(member.name());
            if (found == null) {
                if (member.outline() instanceof Literal) continue;
                // Universal built-in methods (e.g. to_str) are available on every ADT even when
                // not explicitly declared.  Load them lazily and retry before rejecting the check.
                this.loadBuiltInMethods();
                found = this.members.get(member.name());
                if (found == null) return false;
            }
            if (!found.outline().is(member.outline())) {
                return false;
            }
        }
        return true;
    }
}
