package org.twelve.gcp.common;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TupleMatcher {

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
