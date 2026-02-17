package org.twelve.gcp.node.function;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.projectable.Genericable;

import java.util.concurrent.atomic.AtomicLong;

import static org.twelve.gcp.common.Tool.cast;

public class Argument extends Identifier {
    private long index = 0;
    private static AtomicLong indexer = new AtomicLong();

    public static Argument unit(AST ast) {
        return new Argument(new Identifier(ast, Token.unit()), null);
    }

    protected final Expression defaultValue;
    private final TypeNode declared;

    public Argument(Identifier name) {
        this(name, null);
    }

    public Argument(Identifier name, TypeNode declared, Expression defaultValue) {
        super(name.ast(), name.token());
        this.declared = this.addNode(declared);
        this.defaultValue = this.addNode(defaultValue);
        this.index = indexer.incrementAndGet();
    }

    public Argument(Identifier name, TypeNode declared) {
        this(name, declared, null);
    }


    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public Expression defaultValue() {
        return this.defaultValue;
    }

    @Override
    public String lexeme() {
        if(this.outline instanceof UNKNOWN) {
            String ext = "";
            if (this.declared != null) {
                ext = ": " + this.declared.lexeme();
                if (ext.trim().equals(":")) ext = "";
                return "(" + this.name() + ext + ")";
            } else {
                return this.name();
            }
        }else{
            return "(" + this.name() + ":"+this.outline + ")";
        }
    }

    @Override
    public Genericable<?,?> outline() {
        return cast(super.outline());
    }

//    public Argument setIndex(int index) {
//        this.index = index;
//        return this;
//    }

    @Override
    public boolean inferred() {
        boolean result = this.outline.inferred();
        if(!result) ast().missInferred().add(this);
        return result;
    }

    @Override
    public void markUnknowns() {
        if (this.outline instanceof UNKNOWN) {
            GCPErrorReporter.report(this, GCPErrCode.INFER_ERROR);
        }
    }

    @Override
    public long nodeIndex() {
        return this.index;
    }

    public TypeNode declared() {
        return this.declared;
    }
}
