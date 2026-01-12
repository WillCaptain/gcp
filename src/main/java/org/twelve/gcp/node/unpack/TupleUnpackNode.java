package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.TupleNode;
import org.twelve.gcp.node.expression.UnderLineNode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Tuple;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.unpack.Unpack;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class TupleUnpackNode extends UnpackNode {
    protected final List<Node> begins;
    protected final List<Node> ends;

    public TupleUnpackNode(AST ast, List<Node> begins, List<Node> ends) {
        super(ast, null);
        this.begins = begins;
        for (Node begin : this.begins) {
            this.addNode(begin);
        }
        this.ends = ends;
        for (Node end : this.ends) {
            this.addNode(end);
        }
        this.outline = ast.Unknown;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(begins.stream().map(Object::toString).collect(Collectors.joining(", ")));
        if (!ends.isEmpty()) {
            str.append(", ..., ").append(ends.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        return "(" + str + ")";

    }

    /*@Override
    public Tuple outline() {
        List<EntityMember> members = new ArrayList<>();
        for (Integer i = 0; i < this.begins.size(); i++) {
            Node n = this.begins.get(i);
            members.add(EntityMember.from(i.toString(), Generic.from(n, null), Modifier.PUBLIC, false));
        }
        return new Tuple(Tuple.from(this, members));
    }*/

    @Override
    public Outline accept(Inferences inferences) {
        super.accept(inferences);
        return inferences.visit(this);
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
//        if (!(inferred instanceof Tuple)) {
        if (!(inferred.is(this.outline()))) {
            GCPErrorReporter.report(ast(), this, GCPErrCode.OUTLINE_MISMATCH, inferred + " is not unpackable");
            return;
        }
        Tuple tuple = cast(inferred);
        TupleMatcher matcher = new TupleMatcher(tuple);
        for (int i = 0; i < this.begins.size(); i++) {
            Node node = this.begins.get(i);
            if (node instanceof Assignable) {
                ((Assignable) node).assign(env, matcher.match(i));
            }
        }

        for (int i = 0; i < this.ends.size(); i++) {
            Node node = this.ends.get(i);
            int j = i - this.ends.size();
            if (node instanceof Assignable) {
                ((Assignable) node).assign(env, matcher.match(j));
            }
        }

        //Integer counter = assignBegins(begins, tuple, env);
        //assignEnds(counter, ends, tuple, env);
    }

    private Integer assignBegins(List<Node> begins, Tuple tuple, LocalSymbolEnvironment env) {
        if (begins.isEmpty()) return 0;
        for (int i = 0; i < this.begins.size(); i++) {
            if (tuple.size() > i) {
                if (this.begins.get(i) instanceof Assignable) {
                    ((Assignable) this.begins.get(i)).assign(env, tuple.get(i));
                }
            } else {
                GCPErrorReporter.report(this.begins.get(i), GCPErrCode.UNPACK_INDEX_OVER_FLOW);
                return i;
            }
        }
        return this.begins.size();
    }

    private void assignEnds(Integer border, List<Node> ends, Tuple tuple, LocalSymbolEnvironment env) {
        if (this.ends.isEmpty()) return;
        List<Node> reversed = ends.reversed();
        for (int i = 0; i < reversed.size(); i++) {
            if (border + i < tuple.size()) {
                if (reversed.get(i) instanceof Assignable) {
                    ((Assignable) reversed.get(i)).assign(env, tuple.get(tuple.size() - 1 - i));
                }
            } else {
                GCPErrorReporter.report(reversed.get(i), GCPErrCode.UNPACK_INDEX_OVER_FLOW);
                return;
            }
        }
    }

    @Override
    public List<Identifier> identifiers() {
        List<Identifier> ids = new ArrayList<>();
        findIdentifiers(ids, begins);
        findIdentifiers(ids, ends);
        return ids;
    }

    private void findIdentifiers(List<Identifier> ids, List<Node> list) {
        for (Node id : list) {
            if (id instanceof UnpackNode) {
                ids.addAll(((UnpackNode) id).identifiers());
                continue;
            }
            if (!((id instanceof UnderLineNode) || (id instanceof TypeNode))) {
                ids.add(cast(id));
            }
        }
    }

    public List<Node> begins() {
        return this.begins;
    }

    public List<Node> ends() {
        return this.ends;
    }
}

class TupleMatcher {

    private final Tuple tuple;

    public TupleMatcher(Tuple tuple) {
        this.tuple = tuple;
    }

    /**
     * 根据索引在tuple中查找对应的类型
     *
     * @param index 要查找的索引（可以是正数或负数）
     * @return 找到的类型
     * @throws IllegalArgumentException 如果无法找到对应的类型
     */
    public Outline match(Integer index) {
        Map<Integer, Outline> structure = tuple.structure();
        if (index == null || structure == null) {
            throw new IllegalArgumentException("index和tuple不能为null");
        }

        // 情况1：直接匹配
        if (structure.containsKey(index)) {
            return structure.get(index);
        }

        // 情况2：根据索引的正负进行推导匹配
        List<Integer> positiveIndices = getPositiveIndicesSorted(structure.keySet());
        List<Integer> negativeIndices = getNegativeIndicesSorted(structure.keySet());

        // 情况2.1：index是正数，但tuple中没有这个正索引
        if (index >= 0) {
            // 2.1.1: tuple只有负索引的情况
            if (!positiveIndices.isEmpty() && index < positiveIndices.size()) {
                // index在tuple的正索引范围内
                return structure.get(positiveIndices.get(index));
            }

            // 2.1.2: tuple只有负索引，计算对应的负索引位置
            if (!negativeIndices.isEmpty()) {
                // 例如：index=0，tuple有3个负索引，那么对应-3
                int negativeIndex = -(negativeIndices.size() - index);
                if (structure.containsKey(negativeIndex)) {
                    return structure.get(negativeIndex);
                }

                // 或者尝试从负索引列表中找到对应位置
                if (index < negativeIndices.size()) {
                    return structure.get(negativeIndices.get(index));
                }
            }

            // 2.1.3: 混合索引情况，尝试从最后开始匹配
            int totalElements = positiveIndices.size() + negativeIndices.size();
            if (index < totalElements) {
                // 从正索引开始找
                if (index < positiveIndices.size()) {
                    return structure.get(positiveIndices.get(index));
                } else {
                    // 从负索引找
                    int negIndexPos = index - positiveIndices.size();
                    if (negIndexPos < negativeIndices.size()) {
                        return structure.get(negativeIndices.get(negIndexPos));
                    }
                }
            }
        }

        // 情况2.2：index是负数，但tuple中没有这个负索引
        if (index < 0) {
            // 2.2.1: tuple只有正索引的情况
            if (!positiveIndices.isEmpty()) {
                // 计算在正索引中的位置
                // 例如：index=-1，对应最后一个正索引
                int positiveIndexPos = positiveIndices.size() + index;
                if (positiveIndexPos >= 0 && positiveIndexPos < positiveIndices.size()) {
                    return structure.get(positiveIndices.get(positiveIndexPos));
                }
            }

            // 2.2.2: tuple只有负索引，尝试找到相对位置
            if (!negativeIndices.isEmpty()) {
                // 负索引从大到小排序：-1, -2, -3
                // 所以index=-1对应negativeIndices.get(0)
                // index=-2对应negativeIndices.get(1)，以此类推
                int negIndexPos = -index - 1;  // -1 -> 0, -2 -> 1, -3 -> 2
                if (negIndexPos >= 0 && negIndexPos < negativeIndices.size()) {
                    return structure.get(negativeIndices.get(negIndexPos));
                }
            }

            // 2.2.3: 混合索引情况
            // 负索引排在最后面考虑
            // 例如：tuple有正索引0,1和负索引-1
            // index=-1应该对应negativeIndices.get(0)
            if (!negativeIndices.isEmpty()) {
                int negIndexPos = -index - 1;
                if (negIndexPos < negativeIndices.size()) {
                    return structure.get(negativeIndices.get(negIndexPos));
                }
            }
        }

        throw new IllegalArgumentException("无法为索引 " + index + " 找到对应的类型。tuple结构: " + structure);
    }

    /**
     * 获取正索引列表（升序）
     */
    private static List<Integer> getPositiveIndicesSorted(Iterable<Integer> indices) {
        List<Integer> positive = new ArrayList<>();
        for (Integer index : indices) {
            if (index >= 0) {
                positive.add(index);
            }
        }
        Collections.sort(positive);
        return positive;
    }

    /**
     * 获取负索引列表（从大到小：-1, -2, -3...）
     */
    private static List<Integer> getNegativeIndicesSorted(Iterable<Integer> indices) {
        List<Integer> negative = new ArrayList<>();
        for (Integer index : indices) {
            if (index < 0) {
                negative.add(index);
            }
        }
        // 负索引从大到小排序：-1 > -2 > -3
        Collections.sort(negative, (a, b) -> Integer.compare(b, a));
        return negative;
    }
}
