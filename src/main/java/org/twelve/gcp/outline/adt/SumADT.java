package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sum ADT
 * 定义为一种类型可能或者同时拥有多种类型
 * Option表明了多种类型中的一种
 * Poly表明了同时拥有多种类型
 */
public abstract class SumADT extends ADT implements Constrainable, Projectable {
    /**
     * Sum ADT为非标类型，所以有node对应
     */
    private final Node node;
    /**
     * 声明类型组，即在Sum ADT声明时规定的类型
     * 声明后的类型不能再动态添加类型
     */
    protected List<Outline> declared = new ArrayList<>();
    /**
     * Sum ADT拥有的选项
     */
    protected List<Outline> options;


    /**
     * 构造函数
     * outlines为空时表明该Sum ADT可以动态添加options
     *
     * @param node     对应节点
     * @param outlines 可能的options
     */
    SumADT(Node node, AST ast, Outline... outlines) {
        super(ast);
        this.node = node;

        for (Outline outline : outlines) {
            if (outline instanceof SumADT) {
                for (Outline option : ((SumADT) outline).options) {
                    this.sum(this.declared, option);
                }
            } else {
                this.sum(this.declared, outline);
            }
        }
//        this.declared = Arrays.asList(outlines);
        this.options = this.declared.size() == 0 ? new ArrayList<>() : this.declared;
    }

    /**
     * 添加新的option
     * 只对声明为空的Sum ADT有效
     *
     * @param outline 要添加的option
     */
    public Outline sum(Outline outline) {
        //declared seals add Option
        if (!this.declared.isEmpty()) {
//            return this.declared.stream().anyMatch(o -> outline.is(o));
            return this;
        }
        if (outline instanceof SumADT) {
            for (Outline option : ((SumADT) outline).options) {
                this.sum(this.options, option);
            }
        } else {
            this.sum(this.options, outline);
        }
        return this;
    }

    protected boolean sum(List<Outline> list, Outline outline) {
        for (Outline o : list) {
            if (outline instanceof Projectable || o instanceof Projectable) {
                if (outline.equals(o)) return false;//已经有了完全一致的projectable
//                if (outline.id()==o.id()) return false;//已经有了完全一致的projectable
            } else {
                if (outline.is(o)) return false;//已经有基类了，忽略新outline
                if (o.is(outline)) {//outline是基类，去掉已有的，把基类加进来
                    list.remove(o);
                    list.add(outline);
                    return false;
                }
            }
        }
        list.add(outline);
        return true;

    }

    public Node node() {
        return this.node;
    }

    public List<Outline> options() {
        return this.options;
    }

    /**
     * 找到Poly里与参数匹配的option
     * 找到参数的canbe基类
     * 如果基类只有一个，匹配成功
     * 如果超过一个，但其中有一个是equal的，返回equal的
     * 否则是模棱两可的匹配，返回错误
     *
     * @param outline
     * @return
     */
    public Outline match(Outline outline) {
        List<Outline> matched = this.options.stream().filter(o -> outline.canBe(o)).collect(Collectors.toList());
        if (matched.size() == 0) {
            return null;//未能匹配
        }
        if (matched.size() == 1) {
            return matched.get(0);
        } else {
            Optional<Outline> equals = matched.stream().filter(o -> outline.equals(o)).findFirst();
            if (equals.isPresent()) {
                return equals.get();
            } else {
                return this.ast().Error;//模棱两可的匹配
            }
        }
    }

    @Override
    public boolean inferred() {
        return this.options.stream().allMatch(Outline::inferred);
    }

    @Override
    public boolean addDefinedToBe(Outline defined) {
        boolean result = true;
        for (Outline option : this.options()) {
            if (option instanceof Generalizable) {
                result = result || ((Generalizable) option).addDefinedToBe(defined);
            }
        }
        return result;
    }

    @Override
    public void addExtendToBe(Outline extend) {
        for (Outline option : this.options()) {
            if (option instanceof Generalizable) {
                ((Generalizable) option).addDefinedToBe(extend);
            }
        }
    }

    @Override
    public void addHasToBe(Outline hasto) {
        for (Outline option : this.options()) {
            if (option instanceof Generalizable) {
                ((Generalizable) option).addDefinedToBe(hasto);
            }
        }
    }

    abstract Outline projectMySelf(Outline projection, ProjectSession session);
    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if(projected.id()==this.id()){
            return this.projectMySelf(projection,session);
        }else{
            SumADT sum = this;//this.copy();
            List<Outline> os = new ArrayList<>(sum.options);
            sum.options.clear();
            for (Outline option : os) {
                if(option instanceof Projectable) sum.sum(((Projectable) option).project(projected,projection,session));
                else sum.sum(option);
            }
            return sum;
        }
    }
    protected List<Outline> projectList(List<Outline> list, Projectable projected, Outline projection, ProjectSession session) {
        List<Outline> options = new ArrayList<>();
        for (Outline option : list) {
            if (option instanceof Projectable) {
                options.add(((Projectable) option).project(projected, projection, session));
            } else {
                options.add(option);
            }
        }

        return options;
    }

    protected List<Outline> guessList(List<Outline> list) {
        List<Outline> options = new ArrayList<>();
        for (Outline option : list) {
            if (option instanceof Projectable) {
                options.add(((Projectable) option).guess());
            } else {
                options.add(option);
            }
        }
        return options;
    }

    @Override
    public boolean emptyConstraint() {
        return this.options.stream().anyMatch(o -> (o instanceof Projectable) &&
                ((Projectable) o).emptyConstraint());
    }

    @Override
    public boolean containsGeneric() {
        return this.options.stream().anyMatch(o->o instanceof Projectable && ((Projectable) o).containsGeneric());
    }

    @Override
    public boolean containsUnknown() {
        return super.containsUnknown() || this.options.stream().anyMatch(Outline::containsUnknown);
    }

    @Override
    public boolean containsIgnore() {
        return super.containsIgnore() || this.options.stream().anyMatch(Outline::containsIgnore);
    }
}
