package org.twelve.gcp.outline;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.builtin.*;
import org.twelve.gcp.outline.primitive.*;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
import org.twelve.gcp.outline.projectable.Reference;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.twelve.gcp.common.Tool.cast;

public interface Outline extends Serializable {
    AtomicLong Counter = new AtomicLong(100);
    STRING String = new STRING(null);
    DECIMAL Decimal = new DECIMAL(null);
    DOUBLE Double = new DOUBLE(null);
    FLOAT Float = new FLOAT(null);
    INTEGER Integer = new INTEGER(null);
    LONG Long = new LONG(null);
    BOOL Boolean = new BOOL(null);
    UNIT Unit = UNIT.instance();
    IGNORE Ignore = IGNORE.instance();
    UNKNOWN Unknown = new UNKNOWN();
    NOTHING Nothing = NOTHING.instance();
    NUMBER Number = NUMBER.instance();
    ANY Any = ANY.instance();
    ERROR Error = ERROR.instance();
    UNKNOWN Pending = new UNKNOWN();
//    UNKNOWN UnknownThis = new UNKNOWN();

    Node node();

    default String name() {
        String name = this.getClass().getSimpleName();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    /**
     * Get the string representation of the type for debugging or display purposes.
     *
     * @return String representation of the outline type.
     */
//    @Serialization
//    default String name() {
//        String name = this.getClass().getSimpleName();
//        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
//    }
    default boolean equals(Outline another) {
        return this.is(another) && another.is(this);
    }

    /**
     * 基本outline 满足is关系，未验证扩展属性是否满足
     * 非product adt， maybe==is
     *
     * @param another
     * @return
     */
    default boolean maybe(Outline another) {
        return this.is(another);
    }

    /**
     * is关系决定了一种duck typing的继承关系
     * is决定了我可以是你，即我是你的子类的概念
     * is关系的判定用于函数调用传递变量，如果y.is(x)==true,则f(x)函数调用f(y)合法
     * poly场景下，is与canbe不一致
     *
     * @param another 对方类型
     * @return 我是否是对方类型或者对方类型的子类
     */
    default boolean is(Outline another) {
        return this.tryIamYou(another) || another.tryYouAreMe(this);
    }

    /**
     * canBe
     * 如果b.canBe(a)==true,则a = b合法
     * is关系满足，则一定满足canBe，canBe满足，未必满足is关系
     * 在poly场景下，canBe满足条件更宽泛,不需要验证基本类型
     *
     * @param another 对方类型
     * @return 我是否可以作为对方类型的赋值类型
     */
    default boolean canBe(Outline another) {
        return this.tryIamYou(another) || another.tryYouCanBeMe(this);
    }

    default boolean tryIamYou(Outline another) {
        return false;
    }

    default boolean tryYouAreMe(Outline another) {
        return false;
    }

    default boolean tryYouCanBeMe(Outline another) {
        return this.tryYouAreMe(another);
    }

    default <T extends Outline> T copy() {
        return cast(this);
//        try {
//            return cast(this.getClass().newInstance());
//        } catch (Exception e) {
//            return cast(this);
////            return null;
//        }
    }

    default Outline copy(Map<Long, Outline> cache) {
        Outline copied = cast(cache.get(this.id()));
        if (copied == null) {
            copied = this.copy();
            cache.put(this.id(), copied);
        }
        return copied;
    }

    default boolean beAssignedAble() {
        return true;
    }

    default boolean inferred() {
        return !(this instanceof UNKNOWN);
    }

    long id();

    /**
     * project one reference
     *
     * @param reference  to be projected
     * @param projection the real type for the reference
     * @return the real type
     */
    default Outline project(Reference reference, OutlineWrapper projection) {
        return this;
    }

    default Outline eventual() {
        return this;
    }

    default boolean containsUnknown() {
        return false;
    }

    /**
     * to Poly/interacted entity
     * outline&outline
     * @param another
     * @return
     */
    default Outline interact(Outline another){
        if(another instanceof Poly){
            return another.interact(this);
        }else{
            Poly poly = Poly.create();
           poly =  cast(poly.interact(this).interact(another));
           if(poly.options().size()==1)return poly.options().getFirst();
           else return poly;
        }
    }
}
