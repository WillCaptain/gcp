package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.NOTHING;
import org.twelve.gcp.outline.projectable.ProjectSession;

import java.util.*;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

/**
 * sum adt
 */
public class Option extends SumADT {
    public static Option StringOrNumber = new Option(null, Outline.String, Outline.Number);


    public static Outline from(Node node, Outline... outlines) {
        Outline[] os =
                Arrays.stream(outlines).filter(o -> !(o == Outline.Pending)).toArray(Outline[]::new);
        if(os.length==1) return os[0];
        return new Option(node, os);
    }

    public static Outline from(Outline... outlines){
        return from(null,outlines);
    }

    public Option(Node node, Outline... outlines) {
        super(node, outlines);
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
            if (o.canBe(this)) continue;//自己是对方的基类，略过
            //对方是自己的基类，去掉自己，加自己的基类
            Optional<Outline> son = options.stream().filter(m -> m.canBe(o)).findFirst();
            if (son.isPresent()) {
                this.options.remove(son.get());
            }
            this.options.add(o);
        }
        return this;
    }

    @Override
    public boolean beAssignedAble() {
        return options.stream().allMatch(Outline::beAssignedAble);
    }

    @Override
    public Option copy() {
        Option copies = new Option(this.node());
        copies.id = this.id;
        copies.options.addAll(this.options);
        return copies;
    }

    @Override
    public Option copy(Map<Long, Outline> cache) {
        Option copied = cast(cache.get(this.id()));
        if(copied==null){
            copied = new Option(this.node());
            for (Outline option : this.options) {
                copied.options.add(option.copy(cache));
            }
            cache.put(this.id(),copied);
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
            for (EntityMember m2 : members2) {
                if (!m1.name().equals(m2)) continue;
                Outline o = interact(m1.outline(), m2.outline());
                if (o == Nothing) continue;
                //交集默认mutable为false，并且没有绑定的node
                interacted.add(EntityMember.from(m1.name(), o, m1.modifier().mostPermissive(m2.modifier()), false, null));
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
            BuildInOutline buildIn;
            if (outline1.maybe(outline2)) {
                buildIn = ((ProductADT) outline2).buildIn;
            } else if (outline2.maybe(outline1)) {
                buildIn = ((ProductADT) outline1).buildIn;
            } else {
                buildIn = Any;
            }
            Entity entity = Entity.from(this.node(), buildIn);
            List<EntityMember> members = interactMembers(
                    ((ProductADT) outline1).members(), ((ProductADT) outline2).members());
            entity.addMembers(members);
            return entity;
        }

        return Nothing;
    }

    @Override
    public String toString() {
        if(this.options.stream().anyMatch(o->o instanceof NOTHING)){
            return this.options.stream().filter(o-> !(o instanceof NOTHING))
                    .map(o->o.toString()+"?").collect(Collectors.joining("|"));

        }else {
            return this.options.stream().map(o -> o.toString()).collect(Collectors.joining("|"));
        }
    }

    @Override
    public Outline projectMySelf(Outline projection, ProjectSession session) {
        Option copied = this.copy();
        copied.options.clear();
        for (Outline outline : this.projectList(this.options, this, projection, session)) {
            copied.sum(outline);
        }
        return (copied.options.size()==1) ? copied.options.getFirst():copied;
    }

    @Override
    public Outline guess() {
        Option copied = this.copy();
        copied.options = this.guessList(this.options);
        return copied;
    }
}
