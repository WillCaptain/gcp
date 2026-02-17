package org.twelve.gcp.outline.decorators;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.projectable.ReferAble;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class This extends ProductADT implements ReferAble {
    private ProductADT origin;
    private List<OutlineWrapper> referencesProjections = new ArrayList<>();
   private List<Reference> references = new ArrayList<>();

    public This(ProductADT origin) {
        super(origin.ast(), origin.buildIn());
        this.origin = origin;
    }

    @Override
    public String toString() {
        return "{...}";
    }

    @Override
    public Node node() {
        return this.origin.node();
    }

    @Override
    public boolean containsUnknown() {
        return false;
    }

    @Override
    public ProductADT eventual() {
        if(this.referencesProjections.isEmpty()) {
            return this.origin;
        }else{
            if(this.origin instanceof ReferAble) {
                return cast(((ReferAble) this.origin).project(this.referencesProjections));
            }else{
                GCPErrorReporter.report(this.origin.node(), GCPErrCode.NOT_REFER_ABLE);
                return this.origin;
            }
        }
    }

    @Override
    public This copy(Map<Outline, Outline> cache) {
        return new This(this.origin);
    }

    @Override
    public boolean tryIamYou(Outline another) {
        return this.origin.is(another);
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        return another.is(this.origin);
    }

    @Override
    public void updateThis(ProductADT me) {
        if (this.origin.id() != me.id() && me.is(this.origin)) {
            this.origin = me;
        }
    }

    @Override
    public List<Reference> references() {
        return this.references;
    }

    @Override
    public Outline project(List<OutlineWrapper> projections) {
        this.referencesProjections = projections;
        return this;
    }
}
