package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.node.expression.typeable.OptionTypeNode;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

/**
 * 字面量联合类型
 * LiteralUnion遇到literalNode，直接判定值的匹配，不需要用is关系确定
 * 遇到其他expression，那么用literal的is关系成立
 */
public class LiteralUnion extends ADT {
    private final List<ValueNode> values = new ArrayList();
    private final AbstractNode node;

    private LiteralUnion(AbstractNode node, ValueNode... values) {
        super(node.ast());
        for (ValueNode value : values) {
            this.values.add(value);
        }
        this.node = node;
    }

    public static LiteralUnion from(OptionTypeNode node) {
        return new LiteralUnion(node,node.nodes().stream().toArray(ValueNode[]::new));
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (!(another instanceof LiteralUnion)) return false;
        LiteralUnion you = cast(another);
        for (ValueNode value : this.values) {
            if (!you.values.stream().anyMatch(d -> d.equals(value))) return false;
        }
        return true;
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (another instanceof LiteralUnion) return false;
        if (another.node() == null) return false;
        if (!(another.node() instanceof ValueNode)) return false;
        ValueNode value = cast(another.node());
        return this.values.stream().anyMatch(d -> d.isSame(value));
    }

    @Override
    public AbstractNode node() {
        return this.node;
    }

    public List<ValueNode> values() {
        return this.values;
    }
}
