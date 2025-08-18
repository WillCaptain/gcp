package org.twelve.gcp.ast;

import org.twelve.gcp.exception.GCPError;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.inference.OutlineInferences;
import org.twelve.gcp.node.base.Program;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.namespace.NamespaceNode;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.*;
import org.twelve.gcp.outline.builtin.Module;
import org.twelve.gcp.outline.primitive.*;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an Abstract Syntax Tree (AST) for a single module/program.
 * Manages the program's structure (statements, imports/exports), symbol resolution,
 * and type inference. Integrates with an ASF (Abstract Syntax Forest) for cross-module analysis.
 * <p>
 * Key Responsibilities:
 * - Tracks nodes/scopes via atomic counters for unique IDs.
 * - Maintains a local symbol environment and propagates errors.
 * - Coordinates with a global ASF for inter-module dependencies.
 * <p>
 * Thread Safety: Uses AtomicLong for thread-safe ID generation.
 * @author huizi 2025
 */
public class AST {
    private final Program program;  // Root node of the AST
    private final Long id;  // Unique identifier for this AST

    // Thread-safe counters for node/scope IDs
    private final AtomicLong nodeIndexer = new AtomicLong(-1);
    private final AtomicLong scopeIndexer = new AtomicLong(-1);

    private List<GCPError> errors = new ArrayList<>();  // Compilation errors
    private final Inferences inference;  // Type inference rules
    private final LocalSymbolEnvironment symbolEnv;  // Local symbol table
    private ASF asf;  // Parent Abstract Syntax Forest
    private Set<Long> cache = new HashSet<>();  // Optional: Inference caching mechanism

    public final AtomicLong Counter;
    public final UNIT Unit;
    public final UNKNOWN Unknown;
    public final UNKNOWN Pending;
    public final NOTHING Nothing;
    public final ANY Any;
    public final ERROR Error;
    public final IGNORE Ignore;
    public final STRING String;
    public final DECIMAL Decimal;
    public final DOUBLE Double;
    public final FLOAT Float;
    public final INTEGER Integer;
    public final LONG Long;
    public final BOOL Boolean;
    public final NUMBER Number;
    public final Option StringOrNumber;
    public Option stringOrNumber(Node node){
        return new Option(node, this,this.String, this.Number);
    }
    // Constructors

    public AST(ASF asf) {
        this(new OutlineInferences(), asf);  // Default inference rules
    }

    public AST(Inferences inference, ASF asf) {
        this.inference = inference;
        this.id = nodeIndexer.incrementAndGet();  // Assign unique ID
        this.asf = asf;

        this.Counter = new AtomicLong(100);
        this.Unit = new UNIT(this);
        this.Unknown = new UNKNOWN(this);
        this.Pending = new UNKNOWN(this);
        this.Nothing = new NOTHING(this);
        this.Any = new ANY(this);
        this.Error = new ERROR(this);
        this.Ignore = new IGNORE(this);
        this.String = new STRING(this);
        this.Decimal = new DECIMAL(this);
        this.Double = new DOUBLE(this);
        this.Float = new FLOAT(this);
        this.Integer = new INTEGER(this);
        this.Long = new LONG(this);
        this.Boolean = new BOOL(this);
        this.Number = new NUMBER(this);
        this.StringOrNumber = new Option(null, this,this.String, this.Number);
        initialize();

        this.program = new Program(this);  // Initialize root Program node
        this.symbolEnv = new LocalSymbolEnvironment(this);
    }
    private void initialize(){
        this.String.loadMethods();
        this.Decimal.loadMethods();
        this.Double.loadMethods();
        this.Float.loadMethods();
        this.Integer.loadMethods();
        this.Long.loadMethods();
        this.Boolean.loadMethods();
        this.Number.loadMethods();
    }

    // Core Methods
    public Module infer() {
        this.program.infer(this.inference);  // Trigger type inference
        return this.symbolEnv.module();  // Expose inferred module interface
    }

    public boolean inferred() {
        return this.program.inferred();  // Checks if all types are resolved
    }

    // Error Handling
    public GCPError addError(GCPError error) {
        if(error.node()==null) return error;
        // Deduplicate errors by node ID and error code
        if (errors.stream().noneMatch(e ->
                Objects.equals(e.node().id(), error.node().id()) &&
                        e.errorCode() == error.errorCode())) {
            this.errors.add(error);
            return error;
        }
        return null;
    }

    // Program Structure Manipulation
    public <T extends Statement> T addStatement(T statement) {
        return this.program.body().addStatement(statement);
    }

    public Import addImport(Import imports) {
        return this.program().body().addImport(imports);
    }

    public Export addExport(Export export) {
        return this.program().body().addExport(export);
    }

    // Getters
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

    public LocalSymbolEnvironment symbolEnv() {
        return this.symbolEnv;
    }

    public ASF asf() {
        return this.asf;
    }

    public List<GCPError> errors() {
        return this.errors;
    }

    // Metadata
    public NamespaceNode namespace() {
        return this.program.namespace();
    }

    public String lexeme() {
        return this.program().lexeme();
    }

    public String name() {
        return this.program().moduleName();
    }

    @Override
    public String toString() {
        return this.lexeme();  // Human-readable identifier
    }

    // Utility
    public void markUnknowns() {
        this.program.markUnknowns();  // Flag unresolved nodes as errors
    }

    public Set<Long> cache() {
        return this.cache;
    }

    public void setNamespace(List<Identifier> names) {
        this.program.setNamespace(names);
    }
}
