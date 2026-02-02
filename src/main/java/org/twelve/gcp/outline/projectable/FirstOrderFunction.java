package org.twelve.gcp.outline.projectable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;


/**
 * 正常定义的function
 */
public class FirstOrderFunction extends Function<FunctionNode, Genericable<?, ?>> implements ReferAble {

    @Setter
    @Getter
    private ProjectSession session;
    @NonNull
    private final List<Reference> references;

    private FirstOrderFunction(AST ast, Genericable<?, ?> argument, Returnable returns) {
        super(ast, argument, returns);
        this.references = new ArrayList<>();
    }

    private FirstOrderFunction(FunctionNode node, AST ast, Genericable<?, ?> argument, Returnable returns, List<Reference> references) {
        super(node, ast, argument, returns);
        this.references = references == null ? new ArrayList<>() : references;
    }

    public static FirstOrderFunction from(FunctionNode node, Genericable<?, ?> argument, Returnable returns) {
        return new FirstOrderFunction(node, node.ast(), argument, returns, new ArrayList<>());
    }

    public static FirstOrderFunction from(AST ast, List<Reference> refs, Outline returns, Outline... args) {
        if (args.length > 1) {
            Outline arg = args[0];
            Outline[] rests = new Outline[args.length - 1];
            for (int i = 0; i < rests.length; i++) {
                rests[i] = args[i + 1];
            }
            Returnable r = Return.from(ast, from(ast, returns, rests));
            r.addReturn(returns.ast().Nothing);
            return new FirstOrderFunction(null, ast, Generic.from(ast, arg), r, refs);
        } else {
            Returnable r = returns instanceof Returnable ? cast(returns) : Return.from(ast, returns);
            r.addReturn(ast.Nothing);
            return new FirstOrderFunction(null, ast, Generic.from(ast, args[0]), r, refs);
        }
    }


    public static FirstOrderFunction from(AST ast, Outline returns, Outline... args) {
        return from(ast, null, returns, args);
    }

    public static FirstOrderFunction from(FunctionNode node, Genericable<?, ?> argument, Returnable returns, List<Reference> references) {
        return new FirstOrderFunction(node, node.ast(), argument, returns, references);
    }

    public static FirstOrderFunction from(AST ast, Genericable<?, ?> argument, Returnable returns, List<Reference> references) {
        return new FirstOrderFunction(null, ast, argument, returns, references);
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline argProjection = this.argument.project(projected, projection, session);
        Genericable<?, ?> argument = argProjection instanceof Genericable<?, ?> ? cast(argProjection) : Generic.from(this.ast(), argProjection);
        Outline r = this.returns.project(projected, projection, session);
        Returnable returns = (r instanceof Returnable) ? cast(r) : Return.from(this.node, this.ast(), this.returns.declaredToBe());
        return new FirstOrderFunction(this.node, this.ast(), argument, returns, new ArrayList<>());
    }

    @Override
    public FirstOrderFunction copy() {
        Map<Outline, Outline> cache = new HashMap<>();
        return copy(cache);

    }

    @Override
    public FirstOrderFunction copy(Map<Outline, Outline> cache) {
        if (cache.containsKey(this.id())) return cast(cache.get(this.id()));
        //clone all references
        List<Reference> refs = new ArrayList<>();
        for (Reference ref : this.references) {
            refs.add(cast(ref.copy(cache)));
        }
        Genericable<?, ?> arg = cast(this.argument().copy(cache));
        return new FirstOrderFunction((FunctionNode) this.node(), this.ast(), arg, cast(this.returns().copy(cache)), refs);
    }

    @Override
    public List<Reference> references() {
        return this.references;
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        List<Reference> refs = new ArrayList<>(this.references);
//        for (Pair<Reference, Outline> projection : projections) {
        refs.removeIf(r -> r.id() == reference.id());
//        }
        Genericable<?, ?> arg = this.argument();
        Returnable ret = this.returns();
        Outline projected = arg.project(reference, projection);
        if (projected instanceof Genericable<?, ?>) {
            arg = cast(projected);
        } else {
//            arg = Generic.from(this.node.argument(), projected);
            arg = Generic.from(this.argument().node(), projected);
        }
        Outline projectedRet = ret.project(reference, projection);
        if(projectedRet instanceof Returnable){
            ret = cast(projectedRet);

        }else{
            ret = Return.from(ret.node());
            ret.addReturn(projectedRet);
        }
        return new FirstOrderFunction(this.node, this.ast(), arg, ret, refs);
    }

//    @Override
//    public Outline eventual() {
//        Genericable<?,?> arg = cast(this.argument().eventual());
//        Return ret = cast(this.returns().eventual());
//        if (arg != this.argument() || ret != this.returns()) {
//            return new FirstOrderFunction(this.node, arg, ret);
//        } else {
//            return this;
//        }
//    }

    @Override
    public Outline project(List<OutlineWrapper> types) {
        FirstOrderFunction f = this;
        if (this.references.size() != types.size()) {
            GCPErrorReporter.report(this.node, GCPErrCode.REFERENCE_MIS_MATCH);
        }
//        List<Pair<Reference, Outline>> projections = new ArrayList<>();
        for (int i = 0; i < this.references.size(); i++) {
            Reference me = this.references.get(i);
            OutlineWrapper you = types.get(i);
            if (you == null) break;
            if (you.outline().is(me)) {
                f = cast(f.project(me, you));
            } else {
                GCPErrorReporter.report(you.node(), GCPErrCode.REFERENCE_MIS_MATCH);
                f = cast(f.project(me, new OutlineWrapper(you.node(), me.guess())));
            }
        }

        return f.eventual();
    }

}
