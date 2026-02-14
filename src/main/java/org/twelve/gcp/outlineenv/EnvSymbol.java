package org.twelve.gcp.outlineenv;

import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.concurrent.atomic.AtomicLong;

public class EnvSymbol {
    private static AtomicLong counter = new AtomicLong();
    private final boolean mutable;
    private final Long scope;
    private final String name;
    private final Identifier identifier;
    private Outline declared;
//    private boolean isDeclared;
    private Outline outline;
    private final SYMBOL_CATEGORY category;

    public EnvSymbol(String name, boolean mutable, Outline outline, Long scope, Identifier identifier) {
        this.mutable = mutable;
        this.declared = outline;
        this.outline = outline;
        this.name = name;
//        this.isDeclared = isDeclared;
        this.scope = scope;
        this.identifier = identifier;
        this.category = SYMBOL_CATEGORY.VARIABLE;
    }
    public EnvSymbol(String name, Outline outline,Long scope, Identifier identifier) {
        this.mutable = false;
        this.declared = this.outline = outline;
        this.name = name;
//        this.isDeclared = true;
        this.scope = scope;
        this.identifier = identifier;
        this.category = SYMBOL_CATEGORY.OUTLINE;
    }

    public Long id(){
        return this.identifier.id();
    }
    public Long scope() {
        return this.scope;
    }

    public String name() {
        return this.name;
    }

    public boolean mutable() {
        return this.mutable;
    }

    /**
     * 初始化变量时定义变量类型
     *
     * @param outline 变量类型
     * @return 是否定义成功
     */
    public boolean update(Outline outline) {
        if (outline == null || (outline instanceof UNKNOWN) || !outline.beAssignable()) {
            return false;
        }
        if(this.declared.containsUnknown()){
            this.declared = outline;
        }
        this.outline = outline;
        //如果对方是poly或者option，说明是简约显式声明，应记为declare sum adt，后续不可以动态加option
//        if(outline instanceof SumADT){
//            this.isDeclared = true;
//        }
        return true;
    }

    /**
     * 变量重载定义，两个let/var导致变量类型变化
     *
     * @param outline
     * @return
     */
    public boolean polyTo(Poly outline) {
        if (!this.outline.canBe(outline)) return false;
        this.outline = outline;
        return true;
    }

    public Outline outline() {
        return this.outline;
    }

    public Identifier node() {
        return this.identifier;
    }

    public boolean isDeclared() {
//        return this.identifier.isDeclared();
        return !(this.declared instanceof UNKNOWN);
    }

    public Outline declared(){
//        return this.identifier.declared();
        return this.declared;
    }

    @Override
    public String toString() {
        return this.name + ":" + this.outline.toString();
    }
}
