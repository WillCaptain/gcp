package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

import java.util.*;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

/**
 * sum adt
 */
public class Option extends SumADT {
    public static Outline from(Node node, AST ast, Outline... outlines) {
        Outline[] os =
                Arrays.stream(outlines).filter(o -> !(o == ast.Pending)).toArray(Outline[]::new);
        if (os.length == 1) return os[0];
        Option option =  new Option(node, ast, os);
        if(option.options.size()==1) return option.options.getFirst();
        else return option;
    }

    public static Outline from(Node node, Outline... outlines) {
        return from(node, node.ast(), outlines);
    }

//    public static Outline from(AST ast, Outline... outlines) {
//        return from(null, ast, outlines);
//    }

    public Option(Node node, AST ast, Outline... outlines) {
        super(node, ast, outlines);
    }


    @Override
    public Outline sum(Outline outline) {
//        if (!super.sum(outline)) return false;
        //添加新的option
        List<Outline> os = new ArrayList<>();
        if (outline instanceof Option) {
            os.addAll(((Option) outline).options());
        } else {
            os.add(outline);
        }

        for (Outline o : os) {
            super.sum(o);
            /*
            if (o.canBe(this)) continue;//自己是对方的基类，略过
            //对方是自己的基类，去掉自己，加自己的基类
            Optional<Outline> son = options.stream().filter(m -> m.canBe(o)).findFirst();
            if (son.isPresent()) {
                this.options.remove(son.get());
            }
            this.options.add(o);
             */
        }
        return this;
    }

    @Override
    public boolean beAssignedAble() {
        return options.stream().allMatch(Outline::beAssignedAble);
    }

    @Override
    public Option copy() {
        Option copies = new Option(this.node(), this.ast());
        copies.id = this.id;
        copies.options.addAll(this.options);
        return copies;
    }

    @Override
    public Option copy(Map<Outline, Outline> cache) {
        Option copied = cast(cache.get(this));
        if (copied == null) {
            copied = new Option(this.node(), this.ast());
            for (Outline option : this.options) {
                copied.options.add(option.copy(cache));
            }
            cache.put(this, copied);
        }
        return copied;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another instanceof Option) {
            Option o = cast(another);
            boolean is = true;
            for (int i = 0; i < this.options.size(); i++) {
                is = is && this.options.get(i).is(o.options.get(i));
            }
            return is;
        } else {
            return this.out().is(another);
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        for (Outline option : this.options) {
            if (another.is(option)) return true;
        }
        return false;
    }

//    @Override

    /**
     * 可使用类型，{name:String,age:Number}|{name:String,height:Number}
     * 其可使用类型为{name:String}
     */
    private Outline out() {
        Outline out = null;
        for (Outline option : this.options) {
            if (out == null) {
                out = option;
            } else {
                out = interact(out, option);
            }
        }
        return out;
    }


    private List<EntityMember> interactMembers(List<EntityMember> members1, List<EntityMember> members2) {
        List<EntityMember> interacted = new ArrayList<>();
        for (EntityMember m1 : members1) {
            if (m1.isDefault()) continue;
            for (EntityMember m2 : members2) {
                if (!m1.name().equals(m2)) continue;
                if (m2.isDefault()) continue;
                ;
                Outline o = interact(m1.outline(), m2.outline());
                if (o == this.ast().Nothing) continue;
                //交集默认mutable为false，并且没有绑定的node
                interacted.add(EntityMember.from(m1.name(), o, m1.modifier().mostPermissive(m2.modifier()), false, null, false));
            }
        }
//        members1.forEach((k, v1) -> {
//            List<Outline> v2 = members2.get(k);
//            List<Outline> outlines = interacted.put(k, new ArrayList<>());
//            for (int i = 0; i < v2.size(); i++) {
//                outlines.add(interact(v1.get(i), v2.get(i)));
//            }
//        });
        return interacted;
    }

    /**
     * 取得两个outline的交集
     *
     * @param outline1
     * @param outline2
     * @return 交集结果
     */
    private Outline interact(Outline outline1, Outline outline2) {
        if (outline1.is(outline2)) return outline2;
        if (outline2.is(outline1)) return outline1;

        if (outline1 instanceof ProductADT && outline2 instanceof ProductADT) {
            List<EntityMember> members = interactMembers(
                    ((ProductADT) outline1).members(), ((ProductADT) outline2).members());
            return Entity.from(this.node(), members);
        }

        return this.ast().Nothing;
    }

    @Override
    public String toString() {
        if (this.options.stream().anyMatch(o -> o instanceof NOTHING)) {
            return this.options.stream().filter(o -> !(o instanceof NOTHING))
                    .map(o -> o.toString() + "?").collect(Collectors.joining("|"));

        } else {
            return this.options.stream().map(o -> o.toString()).collect(Collectors.joining("|"));
        }
    }

    @Override
    public Outline projectMySelf(Outline projection, ProjectSession session) {
        for (Outline option : this.options) {
            if (projection.is(option)) {
                if (option instanceof Projectable) {
                    return ((Projectable) option).project(cast(option), projection, session);
                }else {
                    return projection;
                }
            }
        }

//        if (projection.is(this)) {
//            return projection;
//        } else {
        GCPErrorReporter.report(this.ast(), this.node(), GCPErrCode.PROJECT_FAIL, projection + " is not " + this);
        return this.guess();
//        }
//        Option copied = this.copy();
//        copied.options.clear();
//        for (Outline outline : this.projectList(this.options, this, projection, session)) {
//            copied.sum(outline);
//        }
//        return (copied.options.size() == 1) ? copied.options.getFirst() : copied;
    }

    @Override
    public Outline guess() {
        Option copied = this.copy();
        copied.options = this.guessList(this.options);
        return copied;
    }
}
