package org.twelve.gcp.ast;

import org.twelve.gcp.exception.GCPError;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.inference.OutlineInferences;
import org.twelve.gcp.node.base.Program;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.namespace.NamespaceNode;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.outline.builtin.Module;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * outline ast
 */
public class AST {
    private final Program program;
    private final Long id;

    private final AtomicLong nodeIndexer = new AtomicLong(-1);
    private final AtomicLong scopeIndexer = new AtomicLong(-1);
    private List<GCPError> errors = new ArrayList<>();
    private final Inferences inference;
    private final LocalSymbolEnvironment symbolEnv;
    private ASF asf;
    private Set<Long> cache = new HashSet<>();
    public AST(ASF asf) {
        this(new OutlineInferences(), asf);
    }

    public AST(Inferences inference, ASF asf) {
        this.inference = inference;
        this.id = nodeIndexer.incrementAndGet();
        this.program = new Program(this);
        this.symbolEnv = new LocalSymbolEnvironment(this.program.scope());
        this.asf = asf;
    }

    public Program program() {
        return this.program;
    }

    public Long id() {
        return this.id;
    }

    public AtomicLong nodeIndexer() {
        return this.nodeIndexer;
    }

    public AtomicLong scopeIndexer() {
        return this.scopeIndexer;
    }

    public NamespaceNode namespace() {
        return this.program.namespace();
    }

    public String lexeme() {
        return this.program().lexeme();
    }

    public String name() {
        return this.program().moduleName();
    }

    public void addError(GCPError error) {
        if(errors.stream().anyMatch(e->e.node().id()==error.node().id() && e.errorCode()==error.errorCode())) return;
        this.errors.add(error);
    }

    public List<GCPError> errors() {
        return this.errors;
    }

    public Module infer() {
//        this.cache.clear();
        this.program.infer(this.inference);
        return this.symbolEnv.module();
    }

    public boolean inferred() {
        return this.program.inferred();
//        return this.errors.size() == 0;
    }

    public LocalSymbolEnvironment symbolEnv() {
        return this.symbolEnv;
    }

    public ASF asf() {
        return this.asf;
    }

    public <T extends Statement> T addStatement(T statement) {
        return this.program.body().addStatement(statement);
    }

    public void setNamespace(List<Token> names) {
        this.program.setNamespace(names);
    }

    public Import addImport(Import imports) {
        return this.program().body().addImport(imports);
    }

    public Export addExport(Export export) {
        return this.program().body().addExport(export);
    }

    @Override
    public String toString() {
        return this.lexeme();
    }

    public void markUnknowns() {
        this.program.markUnknowns();
    }

    public Set<Long> cache() {
        return this.cache;
    }
}
