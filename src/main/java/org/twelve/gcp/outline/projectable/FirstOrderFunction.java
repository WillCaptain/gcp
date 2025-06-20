package org.twelve.gcp.outline.projectable;

import lombok.Getter;
import lombok.Setter;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;

import java.util.ArrayList;
import java.util.List;

import static org.twelve.gcp.common.Tool.cast;


/**
 * 正常定义的function
 */
public class FirstOrderFunction extends Function<FunctionNode, Genericable<?,?>> implements ReferAble {

    @Setter
    @Getter
    private ProjectSession session;
    private final List<Reference> references;

    private FirstOrderFunction(FunctionNode node, Genericable<?,?> argument, Returnable returns) {
        this(node, argument, returns, new ArrayList<>());
    }

    private FirstOrderFunction(FunctionNode node, Genericable<?,?> argument, Returnable returns, List<Reference> references) {
        super(node, argument, returns);
        this.references = references;
    }

    public static FirstOrderFunction from(FunctionNode node, Genericable<?,?> argument, Returnable returns) {
        return new FirstOrderFunction(node, argument, returns, new ArrayList<>());
    }

    public static FirstOrderFunction from(Outline returns, Outline... args) {
        if (args.length > 1) {
            Outline arg = args[0];
            Outline[] rests = new Outline[args.length - 1];
            for (int i = 0; i < rests.length; i++) {
                rests[i] = args[i + 1];
            }
            Returnable r = Return.from(from(returns, rests));
            r.addReturn(Nothing);
            return new FirstOrderFunction(null, Generic.from(arg), r, null);
        } else {
            Returnable r = Return.from(returns);
            r.addReturn(Nothing);
            return new FirstOrderFunction(null, Generic.from(args[0]), r, null);
        }
    }

    public static FirstOrderFunction from(FunctionNode node, Genericable<?,?> argument, Returnable returns, List<Reference> references) {
        return new FirstOrderFunction(node, argument, returns, references);
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline argProjection = this.argument.project(projected, projection, session);
        Genericable<?,?> argument = argProjection instanceof Genericable<?,?> ? cast(argProjection) : Generic.from(argProjection);
        Outline r = this.returns.project(projected, projection, session);
        Returnable returns = (r instanceof Returnable) ? cast(r) : Return.from(this.node, this.returns.declaredToBe());
        return new FirstOrderFunction(this.node, argument, returns);
    }

    @Override
    public FirstOrderFunction copy() {
        return new FirstOrderFunction(this.node, this.argument, this.returns);
    }

    @Override
    public List<Reference> references() {
        return this.references;
    }

    @Override
    public Outline project(Reference reference,OutlineWrapper projection) {
        List<Reference> refs = new ArrayList<>(this.references);
//        for (Pair<Reference, Outline> projection : projections) {
            refs.removeIf(r -> r.id() == reference.id());
//        }
        Genericable<?,?> arg = this.argument();
        Returnable ret = this.returns();
        Outline projected = arg.project(reference, projection);
        if(projected instanceof Genericable<?,?>){
            arg = cast(projected);
        }else {
            arg = Generic.from(this.node.argument(),projected);
        }
        ret = cast(ret.project(reference,projection));
        return new FirstOrderFunction(this.node, arg, ret, refs);
    }

    @Override
    public Outline eventual() {
        Genericable<?,?> arg = cast(this.argument().eventual());
        Return ret = cast(this.returns().eventual());
        if (arg != this.argument() || ret != this.returns()) {
            return new FirstOrderFunction(this.node, arg, ret);
        } else {
            return this;
        }
    }

    @Override
    public Outline project(List<OutlineWrapper> types) {
        FirstOrderFunction f = this;
        if (this.references.size() != types.size()) {
            ErrorReporter.report(this.node, GCPErrCode.REFERENCE_MIS_MATCH);
        }
//        List<Pair<Reference, Outline>> projections = new ArrayList<>();
        for (int i = 0; i < this.references.size(); i++) {
            Reference me = this.references.get(i);
            OutlineWrapper you = types.get(i);
            if (you == null) break;
            if (you.outline().is(me)) {
                f = cast(f.project(me,you));
            }else {
                ErrorReporter.report(you.node(), GCPErrCode.REFERENCE_MIS_MATCH);
                f = cast(f.project(me,new OutlineWrapper(you.node(),me.guess())));
            }
        }

        return f.eventual();
    }

}
