package org.twelve.gcp.ast;

import org.twelve.gcp.exception.GCPError;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.inference.OutlineInferencer;
import org.twelve.gcp.interpreter.OutlineInterpreter;
import org.twelve.gcp.node.base.Program;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.namespace.NamespaceNode;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.*;
import org.twelve.gcp.outline.builtin.Module;
import org.twelve.gcp.outline.primitive.*;
import org.twelve.gcp.meta.MetaExtractor;
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
    private Set<String> errorKeys = new HashSet<>();    // O(1) dedup guard for addError()
    private final Inferencer inferencer;  // Type inference rules
    private final LocalSymbolEnvironment symbolEnv;  // Local symbol table
    private ASF asf;  // Parent Abstract Syntax Forest
    private Set<Long> cache = new HashSet<>();  // Optional: Inference caching mechanism

    public final AtomicLong Counter;
    public final UNIT Unit;
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
    private List<Node> missInferred = new ArrayList<>();
    private String sourceCode;
    /**
     * Syntax-level diagnostics collected during resilient parsing.
     * These are pre-type-inference errors (e.g. unexpected token, reserved keyword,
     * unclosed brace) that the parser recovered from via panic-mode recovery.
     * Stored as plain messages so this module has no hard dependency on MSLL.
     */
    private List<String> syntaxErrors = new ArrayList<>();
    public final UNKNOWN unknown(Node node){
        return new UNKNOWN((AbstractNode) node);
    }
    public final UNKNOWN unknown(){
        return new UNKNOWN(this);
    }
    public Option stringOrNumber(Node node){
        return (Option) Option.from(node,this.String, this.Number);
    }
    // Constructors


    public AST(ASF asf) {
        this(new OutlineInferencer(), asf);  // Default inference rules
    }
    public AST(Inferencer inferencer, ASF asf) {
        this.inferencer = inferencer;
        this.id = nodeIndexer.incrementAndGet();  // Assign unique ID
        this.asf = asf;

        this.Counter = new AtomicLong(100);
        this.Unit = new UNIT(this);
        //this.Unknown = new UNKNOWN(this);
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
//                new Option(null, this,this.String, this.Number);
        initialize();

        this.program = new Program(this);  // Initialize root Program node
        this.symbolEnv = new LocalSymbolEnvironment(this);
        this.StringOrNumber = stringOrNumber(this.program());
    }

    private void initialize(){
        this.String.loadBuiltInMethods();
        this.Decimal.loadBuiltInMethods();
        this.Double.loadBuiltInMethods();
        this.Float.loadBuiltInMethods();
        this.Integer.loadBuiltInMethods();
        this.Long.loadBuiltInMethods();
        this.Boolean.loadBuiltInMethods();
        this.Number.loadBuiltInMethods();
    }
    // Core Methods
    public Inferencer inferences(){
        return this.inferencer;
    }
    public Module infer() {
        this.program.infer(this.inferencer);  // Trigger type inference
        return this.symbolEnv.module();  // Expose inferred module interface
    }
    /**
     * Note: use {@link OutlineInterpreter#runAst(AST)} for full
     * interpretation.  This convenience method returns unit; it exists so
     * {@code Node.interpret} can be called without a live interpreter during testing.
     */
    public org.twelve.gcp.interpreter.value.Value interpret(){
        return org.twelve.gcp.interpreter.value.UnitValue.INSTANCE;
    }

    public boolean inferred() {
        this.missInferred.clear();
        return this.program.inferred();  // Checks if all types are resolved
    }
    // Error Handling

    public GCPError addError(GCPError error) {
        if(error.node()==null) return error;
        // O(1) dedup: key = nodeId + ":" + stable diagnostic identity
        String key = error.node().id() + ":" + error.dedupKey();
        if (errorKeys.add(key)) {
            this.errors.add(error);
            return error;
        }
        return null;
    }

    /** Removes all errors for the given node id, clearing both the list and the dedup set. */
    public void clearNodeErrors(Long nodeId) {
        if (errors.isEmpty()) return;
        errors.removeIf(e -> {
            if (e.node() != null && Objects.equals(e.node().id(), nodeId)) {
                errorKeys.remove(nodeId + ":" + e.dedupKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Removes errors in range [from, to) from the list AND purges their dedup keys.
     * Use this instead of errors().subList(from, to).clear() so that errorKeys stays in sync.
     */
    public void clearErrors(int from, int to) {
        if (from >= to || errors.isEmpty()) return;
        List<GCPError> toRemove = new ArrayList<>(errors.subList(from, to));
        errors.subList(from, to).clear();
        for (GCPError e : toRemove) {
            if (e.node() != null) {
                errorKeys.remove(e.node().id() + ":" + e.dedupKey());
            }
        }
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
    public List<Node> missInferred() {
        return this.missInferred;
    }

    /**
     * Source code for this module. Set by {@link org.twelve.outline.OutlineParser}
     * when parsing from a string. Used by {@link #meta()} to extract comments.
     */
    public void setSourceCode(String code) {
        this.sourceCode = code;
    }

    public String sourceCode() {
        return this.sourceCode;
    }

    /**
     * Attach syntax error messages collected by the parser during resilient
     * parsing. The messages are usually produced by MSLL's panic-mode recovery,
     * but any source of pre-type-inference diagnostics is acceptable.
     */
    public void setSyntaxErrors(List<String> errors) {
        this.syntaxErrors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
    }

    /**
     * @return an immutable view of syntax-level errors collected by the parser.
     *         Empty if the module parsed cleanly or if resilient parsing was not used.
     */
    public List<String> syntaxErrors() {
        return java.util.Collections.unmodifiableList(this.syntaxErrors);
    }

    /**
     * Navigable, typed metadata tree for this module.
     * <pre>
     * ast.meta()                     → ModuleMeta
     * ast.meta().nodes()             → all declarations (outlines, variables, functions)
     * ast.meta().outlines()          → outline declarations with fields/methods
     * ast.meta().variables()         → let/var declarations with type info
     * ast.meta().find("Country")     → lookup by name
     * ast.meta().toMap()             → JSON-serializable Map
     * </pre>
     * See {@link MetaExtractor}.
     */
    public org.twelve.gcp.meta.ModuleMeta meta() {
        return MetaExtractor.extract(this);
    }
}
