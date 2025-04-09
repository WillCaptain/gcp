package org.twelve.gcp.outlineenv;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.adt.SumADT;

import java.util.concurrent.atomic.AtomicLong;

import static org.twelve.gcp.outline.Outline.Unknown;

public class EnvSymbol {
    private static AtomicLong counter = new AtomicLong();
    private final boolean mutable;
    private final Long scope;
    private final String name;
    private final long id;
    private final Node originNode;
    private boolean isDeclared;
    private Outline outline;

    public EnvSymbol(String name, boolean mutable, Outline outline, boolean isDeclared, Long scope, Node originNode) {
        this.id = counter.incrementAndGet();
        this.mutable = mutable;
        this.outline = outline;
        this.name = name;
        this.isDeclared = isDeclared;
        this.scope = scope;
        this.originNode = originNode;
    }

    public Long scope() {
        return this.scope;
    }

    public String name() {
        return this.name;
    }

    public boolean isMutable() {
        return this.mutable;
    }

    /**
     * 初始化变量时定义变量类型
     *
     * @param outline 变量类型
     * @return 是否定义成功
     */
    public boolean update(Outline outline) {
        if (this.outline != Unknown) return false;
        if (outline == null) return false;
        this.outline = outline;
        //如果对方是poly或者option，说明是简约显式声明，应记为declare sum adt，后续不可以动态加option
        if(outline instanceof SumADT){
            this.isDeclared = true;
        }
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

    public Node originNode() {
        return this.originNode;
    }

    public boolean isDeclared() {
        return this.isDeclared;
    }

    @Override
    public String toString() {
        return this.name + ":" + this.outline.toString();
    }
}
