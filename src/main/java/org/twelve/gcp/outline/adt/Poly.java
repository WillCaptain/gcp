package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.outline.Outline;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

/**
 * poly outline 是特殊的sm adt，与option只能持有一个value不同，poly可以同时持有多个value
 * poly持有的每个类型都可能有不同的mutable属性
 * poly每个类型都可能对应不同的node，这在函数重复定义重载时可更清晰的表达
 */
public class Poly extends SumADT {
    /**
     * meta中记录了poly对应个类型的节点和mutable信息
     */
    private final Map<Outline, Pair<ONode, Boolean>> meta = new HashMap<>();
    /**
     * poly是否mutable
     * 该属性与this.node相关
     * 如果this.node不为空，并且declared为空，说明该变量显性声明为显性动态Poly(通常在函数参数声明时)
     * 那么该mutable标明了后续sum的option的mutable属性
     * 如果this.node为空，说明该变量声明为隐性动态poly，该mutable属性无效，需要通过重载来确定下一个sum outline的mutable
     */
    private final Boolean mutable;

    /**
     * 声明式定义的poly类型这
     * 声明定义意味着该poly不能runtime sum更多类型，所以该poly的所有类型的对应节点和mutable属性是一致的
     *
     * @param node
     * @param mutable
     * @param outlines
     */
    Poly(ONode node, Boolean mutable, Outline... outlines) {
        super(node, outlines);
        //设置option的mutable属性
        for (Outline outline : outlines) {
            if (outline instanceof Poly) {
                for (Outline option : ((Poly) outline).options) {
                    this.attachMeta(option, new Pair<>(node, mutable));
                }
            } else {
                this.attachMeta(outline, new Pair<>(node, mutable));

            }
        }
        this.mutable = mutable;
    }


    /**
     * 变量声明式使用，声明后的poly不能动态sum更多类型
     *
     * @param node     变量节点
     * @param mutable  是否mutable
     * @param outlines 可以poly的类型列表
     * @return 固定option的poly对象
     */
    public static Poly from(ONode node, Boolean mutable, Outline... outlines) {
        return new Poly(node, mutable, outlines);
    }


    /**
     * 动态poly
     * node为空，所以可以通过sum添加override的类型
     *
     * @return 空的poly
     */
    public static Poly create() {
        return new Poly(null, false);
    }

    @Override
    public Poly copy() {
        Poly copied = Poly.from(this.node(), false);
        copied.declared.addAll(this.declared);
        copied.options.addAll(this.options);
        copied.meta.putAll(this.meta);
        return copied;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        //参数传递给Poly，自己必须是Poly，而且可完整替代
        if (another instanceof Poly) {
            for (Outline outline : ((Poly) another).options) {
                if (!options.stream().anyMatch(o -> o.is(outline))) {
                    return false;
                }
            }
            return true;
        } else {
            return options.stream().anyMatch(o -> o.is(another));
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (!(another instanceof Poly)) return false;
        return another.tryIamYou(this);
    }


    @Override
    public boolean tryYouCanBeMe(Outline another) {
        if (another instanceof Poly) {
            return tryYouAreMe(another);
        } else {
            return options.stream().anyMatch(o -> another.canBe(o));
        }
    }

    /**
     * 确定某个option是否mutable
     * 如果找不到，则返回默认值
     *
     * @param outline 要寻找的outline
     * @param mutable 默认mutable值
     * @return 要寻找的outline option是否mutable
     */
    public Boolean isMutable(Outline outline, boolean mutable) {
        if (meta.containsKey(outline)) {
            return meta.get(outline).getValue();
        } else {
            return mutable;
        }
    }

    /**
     * 为poly里某个option添加关联节点，是否mutable元数据支持
     *
     * @param outline 目标option
     * @param meta    为该option设置的元数据信息
     */
    private void attachMeta(Outline outline, Pair<ONode, Boolean> meta) {
        this.meta.put(outline, meta);
    }

    /**
     * 为动态poly添加一个option，同时标明该option对应的节点和是否mutable
     * 此方法用于隐性动态poly声明时： let a = x->x; let a = (x,y)->x+y;
     *
     * @param outline 要添加的option
     * @param node    对应的node
     * @param mutable 该option是否mutable
     * @return 添加是否成功
     */
    public boolean sum(Outline outline, ONode node, Boolean mutable) {
        if (this.node() != null) return false;//有node，说明肯定不是隐式动态poly，不能通过let a = b新增poly.option
        //如果是重复infer，则去掉上一次infer
//        if (node.parent().parent() instanceof VariableDeclarator) {
        for (Outline o : this.meta.keySet()) {
            if (this.meta.get(o).getKey() == node) {
                this.meta.remove(o);
                this.options.remove(o);
                break;
            }
        }
//        }
        boolean result = super.sum(outline);
        if (!result) return false;
        if (outline instanceof Poly) {
//            if (((Poly) outline).node() == null) {
//                return false;
//            }
            for (Outline option : ((Poly) outline).options) {
                this.attachMeta(option, ((Poly) outline).meta.get(option));
            }
        } else {
            this.attachMeta(outline, new Pair(node, mutable));
        }
        return true;
    }

    /**
     * 函数参数设置为poly时，允许通过a = b的方式增加poly的option
     * 此时新option的mutable符合参数的初始mutable设置
     *
     * @param outline 要添加的option
     * @return
     */
    @Override
    public boolean sum(Outline outline) {
        if (this.node() == null) return false;//隐式动态poly，退出
        if (this.declared.size() > 0) return false;//显示静态poly，退出

        if (outline instanceof Poly) {
            Poly poly = cast(outline);
            boolean result = true;
            for (Outline option : poly.options) {
                result = result && this.sum(this.options, option);
            }
            poly.meta.forEach((k, v) -> {
                this.meta.put(k, v);
            });
            return result;
        } else {
            return this.sum(this.options, outline);
        }
    }

    @Override
    public String toString() {
        return options.stream().map(o -> o.toString()).collect(Collectors.joining("$"));
    }
}
